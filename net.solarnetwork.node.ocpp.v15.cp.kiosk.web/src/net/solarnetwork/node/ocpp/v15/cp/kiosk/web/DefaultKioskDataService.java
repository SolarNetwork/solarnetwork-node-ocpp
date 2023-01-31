/* ==================================================================
 * DefaultKioskDataService.java - 23/10/2016 6:31:26 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.ocpp.v15.cp.kiosk.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.EnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.ocpp.v15.cp.ChargeSession;
import net.solarnetwork.node.ocpp.v15.cp.ChargeSessionManager;
import net.solarnetwork.node.ocpp.v15.cp.ChargeSessionMeterReading;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.service.DynamicServiceUnavailableException;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.StringUtils;
import ocpp.v15.cs.Measurand;
import ocpp.v15.cs.ReadingContext;

/**
 * Default implementation of {@link KioskDataService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultKioskDataService
		implements KioskDataService, EventHandler, SettingSpecifierProvider {

	/**
	 * The name used to schedule the {@link KioskDataServiceRefreshJob} as.
	 */
	public static final String KIOSK_REFRESH_JOB_NAME = "OCPP_KioskRefresh";

	/**
	 * The job and trigger group used to schedule the
	 * {@link KioskDataServiceRefreshJob} with.
	 */
	public static final String SCHEDULER_GROUP = "OCPP";

	/**
	 * The interval at which to refresh the kiosk data.
	 */
	public static final long REFRESH_JOB_INTERVAL = 2 * 1000L;

	// model data for kiosk
	private final Map<String, Object> kioskData;

	// a mapping of socket ID -> model data
	private final Map<String, Map<String, Object>> socketDataMap;

	// a cache of socket ID -> data source for meter data
	private final Map<String, DatumDataSource> socketMeterDataSources;

	// last seen PV generation power, by source ID
	private final ConcurrentMap<String, AtomicInteger> pvPowerMap;

	// pv data
	private final Map<String, Object> pvDataMap;

	private List<SocketConfiguration> socketConfigurations;
	private Set<String> pvSourceIdSet;
	private Collection<DatumDataSource> meterDataSources;
	private ChargeSessionManager chargeSessionManager;
	private OptionalService<SimpMessageSendingOperations> messageSendingOps;
	private TaskScheduler scheduler;
	private MessageSource messageSource;
	private TaskExecutor taskExecutor;
	private ScheduledFuture<?> refreshKioskDataTrigger;
	private final AtomicBoolean sessionDataRefreshNeeded = new AtomicBoolean(true);

	private final Logger log = LoggerFactory.getLogger(getClass());

	public DefaultKioskDataService() {
		super();
		socketMeterDataSources = new HashMap<>(2);
		socketDataMap = new ConcurrentHashMap<>(2);
		socketConfigurations = new ArrayList<>(2);
		pvSourceIdSet = new LinkedHashSet<>(1);
		pvPowerMap = new ConcurrentHashMap<>(2);
		pvDataMap = new HashMap<>(2);
		pvDataMap.put("power", new AtomicInteger(0));
		Map<String, Object> kData = new LinkedHashMap<>(8);
		kData.put("socketData", socketDataMap);
		kData.put("pvData", pvDataMap);
		kioskData = Collections.unmodifiableMap(kData);
	}

	@Override
	public void startup() {
		log.info("Starting up OCPP kiosk data service");
		configureKioskRefreshJob(REFRESH_JOB_INTERVAL);
	}

	@Override
	public void shutdown() {
		configureKioskRefreshJob(0);
	}

	/**
	 * Call to notify of any configuration changes, for example on any of the
	 * configured {@link SocketConfiguration} instances.
	 */
	public void configurationChanged(Map<String, ?> properties) {
		refreshSessionDataForConfiguredSockets();
	}

	private synchronized void refreshSessionDataForConfiguredSockets() {
		List<SocketConfiguration> confs = getSocketConfigurations();
		if ( confs == null || confs.isEmpty() ) {
			return;
		}
		for ( SocketConfiguration conf : confs ) {
			final String socketId = conf.getSocketId();
			if ( socketId == null ) {
				continue;
			}
			final String socketKey = conf.getKey();
			if ( socketKey == null ) {
				continue;
			}
			try {
				populateSessionDataForSocket(socketId);
			} catch ( DynamicServiceUnavailableException e ) {
				// refresh again later (via refreshKioskData} in case the OCPP service was not yet available at startup
				sessionDataRefreshNeeded.set(true);
				return;
			}
		}
		sessionDataRefreshNeeded.set(false);
	}

	@Override
	public Map<String, Object> getKioskData() {
		return kioskData;
	}

	@Override
	public void handleEvent(final Event event) {
		// use the task executor if available, to avoid being blacklisted
		TaskExecutor executor = taskExecutor;
		if ( executor != null ) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					handleEventInternal(event);
				}
			});
		} else {
			handleEventInternal(event);
		}
	}

	private void handleEventInternal(Event event) {
		final String topic = event.getTopic();
		if ( topic.equals(ChargeSessionManager.EVENT_TOPIC_SESSION_STARTED)
				|| topic.equals(ChargeSessionManager.EVENT_TOPIC_SESSION_ENDED) ) {
			handleSessionEvent(event);
		} else if ( topic.equals(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED) ) {
			handleDatumCapturedEvent(event);
		}
	}

	private String socketKeyForId(String socketId) {
		List<SocketConfiguration> confs = getSocketConfigurations();
		if ( socketId == null || confs == null || confs.isEmpty() ) {
			return null;
		}
		for ( SocketConfiguration conf : confs ) {
			if ( socketId.equals(conf.getSocketId()) ) {
				return conf.getKey();
			}
		}
		return null;
	}

	private void handleSessionEvent(Event event) {
		final boolean sessionStarted = (ChargeSessionManager.EVENT_TOPIC_SESSION_STARTED
				.equals(event.getTopic()));
		final String sessionId = (String) event
				.getProperty(ChargeSessionManager.EVENT_PROPERTY_SESSION_ID);
		final String socketId = (String) event
				.getProperty(ChargeSessionManager.EVENT_PROPERTY_SOCKET_ID);
		final String socketKey = socketKeyForId(socketId);
		if ( socketKey == null ) {
			return;
		}
		Map<String, Object> sessionData = socketDataMap.get(socketKey);
		if ( sessionStarted && sessionData == null ) {
			sessionData = new HashMap<String, Object>(8);
			sessionData.put("sessionId", sessionId);
			sessionData.put("socketId", socketId);
			Number n = (Number) event.getProperty(ChargeSessionManager.EVENT_PROPERTY_DATE);
			if ( n != null ) {
				sessionData.put("startDate", n);
			}
			sessionData.put("duration", new AtomicLong(0L));
			n = (Number) event.getProperty(ChargeSessionManager.EVENT_PROPERTY_METER_READING_POWER);
			sessionData.put("power", new AtomicInteger(n != null ? n.intValue() : 0));
			n = (Number) event.getProperty(ChargeSessionManager.EVENT_PROPERTY_METER_READING_ENERGY);
			sessionData.put("energyStart", n != null ? n : 0L);
			sessionData.put("energy", new AtomicInteger(0));
			sessionData.put("endDate", new AtomicLong(0));
			socketDataMap.put(socketKey, Collections.unmodifiableMap(sessionData));
		} else if ( sessionData != null ) {
			updateSessionData(sessionData,
					(Number) event.getProperty(ChargeSessionManager.EVENT_PROPERTY_METER_READING_POWER),
					(Number) event.getProperty(ChargeSessionManager.EVENT_PROPERTY_METER_READING_ENERGY),
					(Number) event.getProperty(ChargeSessionManager.EVENT_PROPERTY_DATE));
		}
		postMessage(MESSAGE_TOPIC_KIOSK_DATA, kioskData);
		if ( !sessionStarted ) {
			socketDataMap.remove(socketKey);
		}
	}

	private void handleDatumCapturedEvent(Event event) {
		final Object sourceIdObj = event.getProperty("sourceId");
		if ( sourceIdObj == null ) {
			return;
		}
		String sourceId = sourceIdObj.toString();
		if ( !pvSourceIdSet.contains(sourceId) ) {
			return;
		}
		Object powerObj = event.getProperty("watts");
		if ( !(powerObj instanceof Number) ) {
			return;
		}
		Number power = (Number) powerObj;
		AtomicInteger currPower = pvPowerMap.get(sourceId);
		if ( currPower != null ) {
			currPower.set(power.intValue());
		} else {
			currPower = pvPowerMap.putIfAbsent(sourceId, new AtomicInteger(power.intValue()));
			if ( currPower != null ) {
				currPower.set(power.intValue());
			}
		}
	}

	private void postMessage(String topic, Object payload) {
		SimpMessageSendingOperations ops = (messageSendingOps != null ? messageSendingOps.service()
				: null);
		if ( ops == null ) {
			return;
		}
		ops.convertAndSend(topic, payload);
	}

	private void updateSessionData(Map<String, Object> sessionData, Number powerReading,
			Number energyReading, Number endDate) {
		// update power value
		AtomicInteger power = (AtomicInteger) sessionData.get("power");
		if ( powerReading != null && power != null ) {
			power.set(powerReading.intValue());
		}

		// update energy value
		Number energyStart = (Number) sessionData.get("energyStart");
		AtomicInteger energy = (AtomicInteger) sessionData.get("energy");
		if ( energyReading != null && energyStart != null && energy != null ) {
			int oldEnergy = energy.get();
			int newEnergy = (int) (energyReading.longValue() - energyStart.longValue());
			energy.compareAndSet(oldEnergy, newEnergy);
		}

		// update duration, end date
		Number startDate = (Number) sessionData.get("startDate");
		long durationDate = System.currentTimeMillis();
		AtomicLong end = (AtomicLong) sessionData.get("endDate");
		if ( endDate != null && end != null ) {
			end.compareAndSet(0, endDate.longValue());
			durationDate = end.get();
		}
		AtomicLong duration = (AtomicLong) sessionData.get("duration");
		if ( startDate != null && duration != null ) {
			duration.set(durationDate - startDate.longValue());
		}
	}

	private Map<String, Object> sessionDataForSocket(String socketId) {
		final String socketKey = socketKeyForId(socketId);
		if ( socketKey == null ) {
			return null;
		}
		return socketDataMap.get(socketKey);
	}

	private void populateSessionDataForSocket(String socketId) {
		ChargeSession session = chargeSessionManager.activeChargeSession(socketId);
		if ( session == null ) {
			// no session info for this socket
			return;
		}
		// maybe the node has restarted mid-session; pretend we got a Start event
		Map<String, Object> eventData = new HashMap<String, Object>(8);
		eventData.put(ChargeSessionManager.EVENT_PROPERTY_DATE, session.getCreated().getTime());
		eventData.put(ChargeSessionManager.EVENT_PROPERTY_SESSION_ID, session.getSessionId());
		eventData.put(ChargeSessionManager.EVENT_PROPERTY_SOCKET_ID, session.getSocketId());

		List<ChargeSessionMeterReading> readings = chargeSessionManager
				.meterReadingsForChargeSession(session.getSessionId());
		if ( readings != null && !readings.isEmpty() ) {
			int left = 2;
			for ( ChargeSessionMeterReading reading : readings ) {
				if ( ReadingContext.TRANSACTION_BEGIN.equals(reading.getContext()) ) {
					if ( Measurand.POWER_ACTIVE_IMPORT.equals(reading.getMeasurand()) ) {
						Integer power = Integer.valueOf(reading.getValue());
						eventData.put(ChargeSessionManager.EVENT_PROPERTY_METER_READING_POWER, power);
						left--;
					} else if ( Measurand.ENERGY_ACTIVE_IMPORT_REGISTER
							.equals(reading.getMeasurand()) ) {
						Long energy = Long.valueOf(reading.getValue());
						eventData.put(ChargeSessionManager.EVENT_PROPERTY_METER_READING_ENERGY, energy);
						left--;
					}
				}
				if ( left < 1 ) {
					break;
				}
			}
		}
		handleEvent(new Event(ChargeSessionManager.EVENT_TOPIC_SESSION_STARTED, eventData));
	}

	@Override
	public void refreshKioskData() {
		refreshKioskPvData();
		refreshKioskSocketData();
		postMessage(MESSAGE_TOPIC_KIOSK_DATA, kioskData);
	}

	private void refreshKioskPvData() {
		int totalPower = 0;
		for ( AtomicInteger p : pvPowerMap.values() ) {
			totalPower += p.get();
		}
		AtomicInteger total = (AtomicInteger) pvDataMap.get("power");
		total.set(totalPower);
	}

	private void refreshKioskSocketData() {
		if ( socketConfigurations == null || socketConfigurations.isEmpty() ) {
			return;
		}
		if ( sessionDataRefreshNeeded.get() ) {
			refreshSessionDataForConfiguredSockets();
		}
		for ( SocketConfiguration socketConf : socketConfigurations ) {
			final String socketId = socketConf.getSocketId();
			if ( socketId == null ) {
				continue;
			}
			final Map<String, Object> sessionData = sessionDataForSocket(socketId);
			if ( sessionData == null ) {
				continue;
			}

			// get socket activation state

			DatumDataSource meterDataSource = socketMeterDataSources.get(socketId);
			if ( meterDataSource == null ) {
				for ( DatumDataSource ds : meterDataSources ) {
					if ( socketConf.getMeterDataSourceUID().equals(ds.getUid()) ) {
						meterDataSource = ds;
						// cache the data source mapping as we don't expect it to change
						socketMeterDataSources.put(socketId, ds);
						break;
					}
				}
			}
			if ( meterDataSource == null ) {
				log.warn("Meter data source {} not available for socket {}",
						socketConf.getMeterDataSourceUID(), socketId);
				continue;
			}

			// get meter readings for this socket
			NodeDatum meterData = meterDataSource.readCurrentDatum();
			if ( meterData != null ) {
				Integer w = meterData.asSampleOperations()
						.getSampleInteger(DatumSamplesType.Instantaneous, EnergyDatum.WATTS_KEY);
				Long wh = meterData.asSampleOperations().getSampleLong(DatumSamplesType.Accumulating,
						EnergyDatum.WATT_HOUR_READING_KEY);
				updateSessionData(sessionData, w, wh, null);
			}
		}
	}

	private boolean configureKioskRefreshJob(final long interval) {
		final TaskScheduler sched = scheduler;
		if ( sched == null ) {
			log.warn("No scheduler avaialable, cannot schedule OCPP kiosk refresh job");
			return false;
		}
		if ( refreshKioskDataTrigger != null && !refreshKioskDataTrigger.isDone() ) {
			refreshKioskDataTrigger.cancel(true);
			refreshKioskDataTrigger = null;
		}
		if ( interval < 1 ) {
			return true;
		}
		refreshKioskDataTrigger = sched.scheduleWithFixedDelay(new KioskDataServiceRefreshJob(this),
				interval * 1000L);
		return true;
	}

	@Override
	public String getSettingUid() {
		return getClass().getName();
	}

	@Override
	public String getDisplayName() {
		return "OCPP Kiosk Data Service";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(3);
		results.add(new BasicTextFieldSettingSpecifier(
				"filterableChargeSessionManager.propertyFilters['UID']", "OCPP Central System"));
		results.add(new BasicTextFieldSettingSpecifier("pvSourceIds", ""));

		// dynamic list of SocketConfiguration
		Collection<SocketConfiguration> socketConfs = getSocketConfigurations();
		BasicGroupSettingSpecifier socketConfsGroup = SettingUtils.dynamicListSettingSpecifier(
				"socketConfigurations", socketConfs,
				new SettingUtils.KeyedListCallback<SocketConfiguration>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(SocketConfiguration value,
							int index, String key) {
						BasicGroupSettingSpecifier socketConfGroup = new BasicGroupSettingSpecifier(
								value.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(socketConfGroup);
					}
				});
		results.add(socketConfsGroup);
		return results;
	}

	public FilterableService getFilterableChargeSessionManager() {
		return (chargeSessionManager instanceof FilterableService
				? (FilterableService) chargeSessionManager
				: null);
	}

	/**
	 * Set the collection of all available meter data sources from which to find
	 * the appropriate ones to associate with each configured socket.
	 * 
	 * @param meterDataSources
	 *        The collection of meter data sources.
	 */
	public void setMeterDataSources(Collection<DatumDataSource> meterDataSources) {
		this.meterDataSources = meterDataSources;
	}

	/**
	 * Set the charge session manager to use.
	 * 
	 * @param chargeSessionManager
	 *        The charge session manager.
	 */
	public void setChargeSessionManager(ChargeSessionManager chargeSessionManager) {
		this.chargeSessionManager = chargeSessionManager;
	}

	public List<SocketConfiguration> getSocketConfigurations() {
		return socketConfigurations;
	}

	public void setSocketConfigurations(List<SocketConfiguration> socketConfigurations) {
		this.socketConfigurations = socketConfigurations;
	}

	/**
	 * Get the number of configured {@code socketConfigurations} elements.
	 * 
	 * @return The number of {@code socketConfigurations} elements.
	 */
	public int getSocketConfigurationsCount() {
		List<SocketConfiguration> l = getSocketConfigurations();
		return (l == null ? 0 : l.size());
	}

	/**
	 * Adjust the number of configured {@code socketConfigurations} elements.
	 * 
	 * @param count
	 *        The desired number of {@code socketConfigurations} elements.
	 */
	public void setSocketConfigurationsCount(int count) {
		if ( count < 0 ) {
			count = 0;
		}
		List<SocketConfiguration> l = getSocketConfigurations();
		int lCount = (l == null ? 0 : l.size());
		while ( lCount > count ) {
			l.remove(l.size() - 1);
			lCount--;
		}
		while ( lCount < count ) {
			if ( l == null ) {
				l = new ArrayList<SocketConfiguration>(count);
				setSocketConfigurations(l);
			}
			l.add(new SocketConfiguration());
			lCount++;
		}
	}

	public void setMessageSendingOps(OptionalService<SimpMessageSendingOperations> messageSendingOps) {
		this.messageSendingOps = messageSendingOps;
	}

	public void setScheduler(TaskScheduler scheduler) {
		this.scheduler = scheduler;
	}

	public Set<String> getPvSourceIdSet() {
		return pvSourceIdSet;
	}

	public void setPvSourceIdSet(Set<String> pvSourceIdSet) {
		this.pvSourceIdSet = pvSourceIdSet;
	}

	/**
	 * Get the {@code pvSourceIdSet} as a comma-delimited string.
	 * 
	 * @return The {@link #getPvSourceIdSet()} as a delimited string.
	 */
	public String getPvSourceIds() {
		return StringUtils.commaDelimitedStringFromCollection(this.pvSourceIdSet);
	}

	/**
	 * Set the {@code pvSourceIdSet} from a comma-delimited string.
	 * 
	 * @param value
	 *        A comma-delimited string to parse and pass to
	 *        {@link #setPvSourceIdSet(Set)}.
	 */
	public void setPvSourceIds(String value) {
		this.pvSourceIdSet = StringUtils.commaDelimitedStringToSet(value);
	}

	/**
	 * Set a {@link TaskExecutor} to handle asynchronous operations with.
	 * 
	 * @param taskExecutor
	 *        The task executor.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

}
