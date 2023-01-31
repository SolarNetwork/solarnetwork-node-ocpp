/* ==================================================================
 * MockMeterDataSource.java - 10/06/2015 1:28:07 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.v15.cp.mock;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.ocpp.v15.cp.ChargeSessionManager;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Mock implementation of {@link DatumDataSource} to simulate a power meter used
 * in an OCPP charge point.
 * 
 * @author matt
 * @version 1.0
 */
public class MockMeterDataSource
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider, EventHandler {

	private OptionalService<EventAdmin> eventAdmin;
	private MessageSource messageSource;
	private long sampleCacheMs = 5000;
	private String uid = "MockMeter";
	private String socketId = "/socket/mock";
	private String groupUID;
	private double watts = 5;
	private double wattsRandomness = 0.2;
	private double chargingWatts = 2400;
	private double chargingWattsRandomness = 0.1;
	private boolean charging;

	private SimpleAcEnergyDatum sample;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AtomicLong mockMeter = new AtomicLong(meterStartValue());

	/**
	 * Get a mock starting value for our meter. As we expect meters to only
	 * increase, the value returned here is based on the current time. Thus
	 * starting/stopping this service won't roll the meter back.
	 * 
	 * @return a starting meter value
	 */
	private long meterStartValue() {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		Date now = cal.getTime();
		cal.set(2010, cal.getMinimum(Calendar.MONTH), 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return (now.getTime() - cal.getTimeInMillis()) / (1000L * 60);
	}

	private SimpleAcEnergyDatum getCurrentSample() {
		SimpleAcEnergyDatum currSample = sample;
		if ( isSampleExpired(currSample) ) {
			SimpleAcEnergyDatum newSample = new SimpleAcEnergyDatum(uid, Instant.now(),
					new DatumSamples());
			newSample.setAcPhase(AcPhase.Total);
			if ( currSample == null ) {
				newSample.setWattHourReading(mockMeter.get());
			} else {
				double diffHours = ((newSample.getTimestamp().toEpochMilli()
						- currSample.getTimestamp().toEpochMilli()) / (double) (1000 * 60 * 60));
				double power;
				if ( charging ) {
					power = chargingWatts;
				} else {
					// we expect very little draw (ideally 0, but let's allow for a little)
					power = watts;
				}
				power += (power
						* (Math.random() * (charging ? chargingWattsRandomness : wattsRandomness))
						* (Math.random() < 0.5 ? -1 : 1));
				long wh = (long) (power * diffHours);
				long newWh = currSample.getWattHourReading() + wh;
				if ( mockMeter.compareAndSet(currSample.getWattHourReading(), newWh) ) {
					newSample.setWattHourReading(newWh);
					newSample.setWatts((int) power);
				} else {
					newSample.setWattHourReading(currSample.getWattHourReading());
				}
			}
			log.debug("Read mock data: {}", newSample);
			currSample = newSample;
			sample = newSample;
		}
		return currSample;
	}

	private boolean isSampleExpired(SimpleAcEnergyDatum datum) {
		if ( datum == null ) {
			return true;
		}
		final long lastReadDiff = System.currentTimeMillis() - datum.getTimestamp().toEpochMilli();
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.ocpp.v15.cp.mock.meter";
	}

	@Override
	public String getDisplayName() {
		return getClass().getSimpleName();
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		MockMeterDataSource defaults = new MockMeterDataSource();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);
		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.uid));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.groupUID));
		results.add(new BasicTextFieldSettingSpecifier("socketId", defaults.socketId));

		results.add(new BasicTextFieldSettingSpecifier("watts", String.valueOf(defaults.watts)));
		results.add(new BasicTextFieldSettingSpecifier("wattsRandomness",
				String.valueOf(defaults.wattsRandomness)));
		results.add(new BasicTextFieldSettingSpecifier("chargingWatts",
				String.valueOf(defaults.chargingWatts)));
		results.add(new BasicTextFieldSettingSpecifier("chargingWattsRandomness",
				String.valueOf(defaults.chargingWattsRandomness)));

		return results;
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return getDatumType();
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		return Collections.singletonList(readCurrentDatum());
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return SimpleAcEnergyDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		final Instant start = Instant.now();
		final SimpleAcEnergyDatum d = getCurrentSample();
		if ( d.getTimestamp().isAfter(start) ) {
			// we read from the meter
			postDatumCapturedEvent(d);
		}
		return d;
	}

	/**
	 * Post a {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED} {@link Event}.
	 * 
	 * <p>
	 * This method calls {@link DatumEvents#datumCapturedEvent(NodeDatum)} to
	 * create the actual Event, which may be overridden by extending classes.
	 * </p>
	 * 
	 * @param datum
	 *        the datum to post the event for
	 * @since 2.0
	 */
	protected final void postDatumCapturedEvent(final NodeDatum datum) {
		EventAdmin ea = OptionalService.service(eventAdmin);
		if ( ea == null || datum == null ) {
			return;
		}
		Event event = DatumEvents.datumCapturedEvent(datum);
		ea.postEvent(event);
	}

	@Override
	public void handleEvent(Event event) {
		final String topic = event.getTopic();
		final String socketId = (String) event
				.getProperty(ChargeSessionManager.EVENT_PROPERTY_SOCKET_ID);
		if ( socketId == null || !socketId.equals(this.socketId) ) {
			return;
		}
		if ( ChargeSessionManager.EVENT_TOPIC_SOCKET_ACTIVATED.equals(topic) ) {
			setCharging(true);
			log.info("Mock OCPP meter {} simulating charging load ACTIVATED on socket {}", uid,
					socketId);
		} else if ( ChargeSessionManager.EVENT_TOPIC_SOCKET_DEACTIVATED.equals(topic) ) {
			setCharging(false);
			log.info("Mock OCPP meter {} simulating charging load DEACTIVATED on socket {}", uid,
					socketId);
		}
	}

	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	@Override
	public String getGroupUid() {
		return groupUID;
	}

	public void setGroupUid(String groupUID) {
		this.groupUID = groupUID;
	}

	public boolean isCharging() {
		return charging;
	}

	public void setCharging(boolean charging) {
		this.charging = charging;
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	public String getSocketId() {
		return socketId;
	}

	public void setSocketId(String socketId) {
		this.socketId = socketId;
	}

	public void setWatts(double watts) {
		this.watts = watts;
	}

	public void setWattsRandomness(double wattRandomness) {
		this.wattsRandomness = wattRandomness;
	}

	public void setChargingWatts(double chargingWatts) {
		this.chargingWatts = chargingWatts;
	}

	public void setChargingWattsRandomness(double chargingWattRandomness) {
		this.chargingWattsRandomness = chargingWattRandomness;
	}

}
