/* ==================================================================
 * SolarNetChargeSessionManager.java - 14/02/2020 5:31:00 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.cs.session;

import static java.util.Collections.singleton;
import static net.solarnetwork.node.domain.Datum.REVERSE_ACCUMULATING_SUFFIX_KEY;
import static net.solarnetwork.util.OptionalService.service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.PlaceholderService;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.ocpp.dao.ChargePointDao;
import net.solarnetwork.ocpp.dao.ChargeSessionDao;
import net.solarnetwork.ocpp.dao.PurgePostedChargeSessionsTask;
import net.solarnetwork.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.ocpp.domain.ChargeSessionEndInfo;
import net.solarnetwork.ocpp.domain.ChargeSessionStartInfo;
import net.solarnetwork.ocpp.domain.Location;
import net.solarnetwork.ocpp.domain.Measurand;
import net.solarnetwork.ocpp.domain.Phase;
import net.solarnetwork.ocpp.domain.ReadingContext;
import net.solarnetwork.ocpp.domain.SampledValue;
import net.solarnetwork.ocpp.domain.UnitOfMeasure;
import net.solarnetwork.ocpp.service.AuthorizationException;
import net.solarnetwork.ocpp.service.AuthorizationService;
import net.solarnetwork.ocpp.service.cs.ChargeSessionManager;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * A {@link ChargeSessionManager} that generates {@link Datum} from charge
 * session transaction data.
 * 
 * @author matt
 * @version 1.1
 */
public class SolarNetChargeSessionManager extends BaseIdentifiable
		implements ChargeSessionManager, SettingSpecifierProvider, SettingsChangeObserver {

	/** A datum property name for a charging session ID. */
	public static final String SESSION_ID_PROPERTY = "sessionId";

	/** The default {@code sourceIdTemplate} value. */
	public static final String DEFAULT_SOURCE_ID_TEMPLATE = "/ocpp/cp/{chargerIdentifier}/{connectorId}/{location}";

	/** The default {@code maxTemperatureScale} value. */
	public static final int DEFAULT_MAX_TEMPERATURE_SCALE = 1;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AuthorizationService authService;
	private final ChargePointDao chargePointDao;
	private final ChargeSessionDao chargeSessionDao;
	private final OptionalService<DatumDao<GeneralNodeDatum>> datumDao;
	private String sourceIdTemplate = DEFAULT_SOURCE_ID_TEMPLATE;
	private int maxTemperatureScale = DEFAULT_MAX_TEMPERATURE_SCALE;
	private TaskScheduler taskScheduler;

	private final PurgePostedChargeSessionsTask purgePostedTask = new PurgePostedChargeSessionsTask();
	private ScheduledFuture<?> configurationFuture;
	private ScheduledFuture<?> purgePostedFuture;

	/**
	 * Constructor.
	 * 
	 * @param authService
	 *        the authorization service to use
	 * @param chargePointDao
	 *        the charge point DAO to use
	 * @param chargeSessionDao
	 *        the charge session DAO to use
	 * @param datumDao
	 *        the DAO for saving Datum
	 */
	public SolarNetChargeSessionManager(AuthorizationService authService, ChargePointDao chargePointDao,
			ChargeSessionDao chargeSessionDao, OptionalService<DatumDao<GeneralNodeDatum>> datumDao) {
		super();
		this.authService = authService;
		this.chargePointDao = chargePointDao;
		this.chargeSessionDao = chargeSessionDao;
		this.datumDao = datumDao;
	}

	/**
	 * Initialize after properties configured.
	 */
	public void startup() {
		reconfigure();
	}

	/**
	 * Free resources after no longer needed.
	 */
	public void shutdown() {
		stopTasks();
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		if ( properties == null || properties.isEmpty() ) {
			return;
		}
		reconfigure();
	}

	private synchronized void stopTasks() {
		if ( purgePostedFuture != null ) {
			if ( !purgePostedFuture.isDone() ) {
				purgePostedFuture.cancel(true);
			}
			purgePostedFuture = null;
		}
	}

	private synchronized void reconfigure() {
		if ( taskScheduler != null ) {
			stopTasks();
			if ( configurationFuture != null ) {
				if ( !configurationFuture.isDone() ) {
					configurationFuture.cancel(true);
				}
			}
			configurationFuture = taskScheduler.schedule(new ConfigurationTask(),
					new Date(System.currentTimeMillis() + 1000));
		}
	}

	private final class ConfigurationTask implements Runnable {

		@Override
		public void run() {
			TaskScheduler scheduler = getTaskScheduler();
			if ( scheduler == null ) {
				return;
			}
			synchronized ( SolarNetChargeSessionManager.this ) {
				configurationFuture = null;
				stopTasks();
				int purgeHours = getPurgePostedChargeSessionsExpirationHours();
				if ( purgeHours > 0 ) {
					log.info("Scheduling OCPP posted charge session purge task at {} hours.",
							purgeHours);
					long purgeMs = TimeUnit.HOURS.toMillis(purgeHours) / 4;
					purgePostedFuture = scheduler.scheduleWithFixedDelay(purgePostedTask,
							new Date(System.currentTimeMillis() + purgeMs), purgeMs);
				}
			}
		}

	}

	private ChargePoint chargePoint(ChargePointIdentity identifier, String authId) {
		ChargePoint cp = chargePointDao.getForIdentity(identifier);
		if ( cp == null ) {
			throw new AuthorizationException(
					String.format("ChargePoint %s not available.", identifier.getIdentifier()),
					new AuthorizationInfo(authId, AuthorizationStatus.Invalid));
		}
		return cp;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChargeSession startChargingSession(ChargeSessionStartInfo info)
			throws AuthorizationException {
		// check authorization
		AuthorizationInfo authInfo = authService.authorize(info.getChargePointId(),
				info.getAuthorizationId());
		if ( authInfo == null || AuthorizationStatus.Accepted != authInfo.getStatus() ) {
			throw new AuthorizationException(authInfo);
		}

		ChargePoint cp = chargePoint(info.getChargePointId(), info.getAuthorizationId());

		// check for existing session, e.g. ConcurrentTx
		ChargeSession sess = chargeSessionDao.getIncompleteChargeSessionForConnector(cp.getId(),
				info.getConnectorId());
		if ( sess != null ) {
			throw new AuthorizationException(
					String.format("ChargeSession %s already active for Charge Point %s connector %d",
							sess.getId(), info.getChargePointId(), info.getConnectorId()),
					new AuthorizationInfo(info.getAuthorizationId(), AuthorizationStatus.ConcurrentTx));
		}

		// persist a new session and then re-load to get the generated transaction ID
		try {
			sess = new ChargeSession(UUID.randomUUID(), info.getTimestampStart(),
					info.getAuthorizationId(), cp.getId(), info.getConnectorId(), 0);
			sess = chargeSessionDao.get(chargeSessionDao.save(sess));
		} catch ( DataIntegrityViolationException e ) {
			// assume this is from no matching Charge Point for the given chargePointId value
			throw new AuthorizationException(new AuthorizationInfo(info.getAuthorizationId(),
					AuthorizationStatus.Invalid, null, null));
		}

		// generate Datum from start meter value

		// @formatter:off
		SampledValue reading = SampledValue.builder()
				.withSessionId(sess.getId())
				.withTimestamp(sess.getCreated())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue(String.valueOf(info.getMeterStart()))
				.build();
		// @formatter:on
		chargeSessionDao.addReadings(singleton(reading));
		DatumDao<GeneralNodeDatum> dao = datumDao.service();
		if ( dao != null ) {
			GeneralNodeDatum d = datum(cp, sess, reading);
			if ( d != null ) {
				dao.storeDatum(d);
			}
		}

		return sess;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public ChargeSession getActiveChargingSession(ChargePointIdentity identifier, int transactionId)
			throws AuthorizationException {
		ChargePoint cp = chargePoint(identifier, null);
		return chargeSessionDao.getIncompleteChargeSessionForTransaction(cp.getId(), transactionId);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<ChargeSession> getActiveChargingSessions(ChargePointIdentity identifier) {
		if ( identifier != null ) {
			ChargePoint cp = chargePoint(identifier, null);
			return chargeSessionDao.getIncompleteChargeSessionsForChargePoint(cp.getId());
		}
		return chargeSessionDao.getIncompleteChargeSessions();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AuthorizationInfo endChargingSession(ChargeSessionEndInfo info) {
		ChargePoint cp = chargePoint(info.getChargePointId(), info.getAuthorizationId());
		ChargeSession sess = chargeSessionDao.getIncompleteChargeSessionForTransaction(cp.getId(),
				info.getTransactionId());
		if ( sess == null ) {
			throw new AuthorizationException("No active charging session found.", new AuthorizationInfo(
					info.getAuthorizationId(), AuthorizationStatus.Invalid, null, null));
		}

		sess.setEndAuthId(info.getAuthorizationId());
		sess.setEnded(info.getTimestampEnd());
		sess.setEndReason(info.getReason());
		sess.setPosted(Instant.now());
		chargeSessionDao.save(sess);

		// generate reading from end meter value

		// @formatter:off
		SampledValue reading = SampledValue.builder()
				.withSessionId(sess.getId())
				.withTimestamp(sess.getEnded())
				.withContext(ReadingContext.TransactionEnd)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue(String.valueOf(info.getMeterEnd()))
				.build();
		// @formatter:on

		// add all provided readings, plus our final TransactionEnd reading
		List<SampledValue> readings = new ArrayList<>();
		if ( info.getTransactionData() != null ) {
			for ( SampledValue v : info.getTransactionData() ) {
				readings.add(v);
			}
		}
		readings.add(reading);
		Map<UUID, ChargeSession> sessions = new HashMap<>(2);
		sessions.put(sess.getId(), sess);
		Map<Long, ChargePoint> chargePoints = new HashMap<>(2);
		chargePoints.put(cp.getId(), cp);
		addReadings(readings, sessions, chargePoints);

		return new AuthorizationInfo(info.getAuthorizationId(), AuthorizationStatus.Accepted, null,
				null);
	}

	private GeneralNodeDatum datum(ChargePoint chargePoint, ChargeSession sess, SampledValue reading) {
		GeneralNodeDatum d = new GeneralNodeDatum();
		populateProperty(d, reading.getMeasurand(), reading.getUnit(), reading.getValue());
		if ( d.getSamples() != null && !d.getSamples().isEmpty() ) {
			d.setCreated(new Date(reading.getTimestamp().toEpochMilli()));
			d.setSourceId(sourceId(chargePoint, sess.getConnectorId(), reading.getLocation(),
					reading.getPhase()));
			d.putStatusSampleValue(SESSION_ID_PROPERTY, sess.getId().toString());
			return d;
		}
		return null;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<SampledValue> getChargingSessionReadings(UUID sessionId) {
		return chargeSessionDao.findReadingsForSession(sessionId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addChargingSessionReadings(Iterable<SampledValue> readings) {
		addReadings(readings, new HashMap<>(2), new HashMap<>(2));
	}

	private void addReadings(Iterable<SampledValue> readings, Map<UUID, ChargeSession> sessions,
			Map<Long, ChargePoint> chargePoints) {
		if ( readings == null ) {
			return;
		}
		Map<UUID, Set<SampledValue>> currentReadings = new HashMap<>(2);
		List<SampledValue> sorted = StreamSupport.stream(readings.spliterator(), false).sorted()
				.collect(Collectors.toList());
		List<SampledValue> newReadings = new ArrayList<>();
		for ( SampledValue r : sorted ) {
			Set<SampledValue> current = currentReadings.get(r.getSessionId());
			if ( current == null ) {
				ChargeSession sess = sessions.get(r.getSessionId());
				if ( sess == null ) {
					sess = chargeSessionDao.get(r.getSessionId());
					if ( sess == null ) {
						throw new AuthorizationException("No active charging session found.",
								new AuthorizationInfo(null, AuthorizationStatus.Invalid, null, null));
					}
					sessions.put(r.getSessionId(), sess);
				}
				current = new HashSet<>(getChargingSessionReadings(r.getSessionId()));
				currentReadings.put(r.getSessionId(), current);
			}
			if ( !current.contains(r) ) {
				newReadings.add(r);
			}
		}
		if ( !newReadings.isEmpty() ) {
			chargeSessionDao.addReadings(newReadings);
			DatumDao<GeneralNodeDatum> dao = datumDao.service();
			if ( dao != null ) {
				// group readings by timestamp into Datum
				GeneralNodeDatum d = null;
				for ( SampledValue reading : newReadings ) {
					if ( d == null
							|| d.getCreated().getTime() != reading.getTimestamp().toEpochMilli() ) {
						if ( d != null ) {
							dao.storeDatum(d);
							d = null;
						}

						ChargeSession s = sessions.get(reading.getSessionId());
						ChargePoint cp = chargePoints.get(s.getChargePointId());
						if ( cp == null ) {
							cp = chargePointDao.get(s.getChargePointId());
							if ( cp == null ) {
								throw new AuthorizationException(
										String.format("ChargePoint %d not available.",
												s.getChargePointId()),
										new AuthorizationInfo(s.getAuthId(),
												AuthorizationStatus.Invalid));
							}
							chargePoints.put(cp.getId(), cp);
						}
						d = datum(cp, sessions.get(reading.getSessionId()), reading);
					} else {
						populateProperty(d, reading.getMeasurand(), reading.getUnit(),
								reading.getValue());
					}
				}
				if ( d != null ) {
					dao.storeDatum(d);
				}
			}
		}
	}

	private String sourceId(ChargePoint chargePoint, int connectorId, Location location, Phase phase) {
		Map<String, Object> params = new HashMap<>(4);
		params.put("chargePointId", chargePoint.getId());
		params.put("chargerIdentifier", chargePoint.getInfo().getId());
		params.put("connectorId", connectorId);
		params.put("location", location);
		params.put("phase", phase);
		PlaceholderService service = service(getPlaceholderService());
		return (service != null ? service.resolvePlaceholders(sourceIdTemplate, params)
				: StringUtils.expandTemplateString(sourceIdTemplate, params));
	}

	private void populateProperty(GeneralNodeDatum datum, Measurand measurand, UnitOfMeasure unit,
			Object value) {
		if ( value == null ) {
			return;
		}
		BigDecimal num = null;
		if ( value instanceof Number ) {
			num = NumberUtils.bigDecimalForNumber((Number) value);
		} else {
			try {
				num = new BigDecimal(value.toString());
			} catch ( NumberFormatException e ) {
				log.debug("Error parsing OCPP {} sampled value [{}]: {}", measurand, value,
						e.getMessage());
				return;
			}
		}
		num = normalizedUnit(num, unit);
		String propName = propertyName(measurand);
		if ( propName != null ) {
			datum.putSampleValue(propertyType(measurand), propName, num);
		}
	}

	private BigDecimal normalizedUnit(BigDecimal num, UnitOfMeasure unit) {
		if ( unit == null ) {
			return num;
		}
		switch (unit) {
			case Fahrenheit: {
				// convert to C
				BigDecimal celsius = num.subtract(new BigDecimal("32")).multiply(new BigDecimal("5"))
						.divide(new BigDecimal("9"));
				if ( maxTemperatureScale >= 0 && celsius.scale() > maxTemperatureScale ) {
					celsius = celsius.setScale(maxTemperatureScale, RoundingMode.HALF_UP);
				}
				return celsius;
			}

			case K: {
				BigDecimal celsius = num.subtract(new BigDecimal("-273.15"));
				if ( maxTemperatureScale >= 0 && celsius.scale() > maxTemperatureScale ) {
					celsius = celsius.setScale(maxTemperatureScale, RoundingMode.HALF_UP);
				}
				return celsius;
			}

			case kVA:
			case kvar:
			case kvarh:
			case kW:
			case kWh:
				return num.movePointRight(3);

			default:
				return num;
		}
	}

	private GeneralDatumSamplesType propertyType(Measurand measurand) {
		switch (measurand) {
			case EnergyActiveExportRegister:
			case EnergyActiveImportRegister:
			case EnergyReactiveExportRegister:
			case EnergyReactiveImportRegister:
			case PowerReactiveExport:
			case PowerReactiveImport:
				return GeneralDatumSamplesType.Accumulating;

			default:
				return GeneralDatumSamplesType.Instantaneous;
		}
	}

	private String propertyName(Measurand measurand) {
		switch (measurand) {
			case CurrentExport:
				return ACEnergyDatum.CURRENT_KEY + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case CurrentImport:
				return ACEnergyDatum.CURRENT_KEY;

			case CurrentOffered:
				return ACEnergyDatum.CURRENT_KEY + "Offered";

			case EnergyActiveExportInterval:
				return ACEnergyDatum.WATT_HOUR_READING_KEY + "Diff" + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case EnergyActiveExportRegister:
				return ACEnergyDatum.WATT_HOUR_READING_KEY + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case EnergyActiveImportInterval:
				return ACEnergyDatum.WATT_HOUR_READING_KEY + "Diff";

			case EnergyActiveImportRegister:
				return ACEnergyDatum.WATT_HOUR_READING_KEY;

			case EnergyReactiveExportInterval:
				return "reactiveEnergyDiff" + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case EnergyReactiveExportRegister:
				return "reactiveEnergy" + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case EnergyReactiveImportInterval:
				return "reactiveEnergyDiff";

			case EnergyReactiveImportRegister:
				return "reactiveEnergy";

			case Frequency:
				return ACEnergyDatum.FREQUENCY_KEY;

			case PowerActiveExport:
				return ACEnergyDatum.WATTS_KEY + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case PowerActiveImport:
				return ACEnergyDatum.WATTS_KEY;

			case PowerFactor:
				return ACEnergyDatum.POWER_FACTOR_KEY;

			case PowerOffered:
				return ACEnergyDatum.WATTS_KEY + "Offered";

			case PowerReactiveExport:
				return ACEnergyDatum.REACTIVE_POWER_KEY + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case PowerReactiveImport:
				return ACEnergyDatum.REACTIVE_POWER_KEY;

			case RPM:
				return "rpm";

			case SoC:
				return "soc";

			case Temperature:
				return AtmosphericDatum.TEMPERATURE_KEY;

			case Voltage:
				return ACEnergyDatum.VOLTAGE_KEY;

			default:
				return null;
		}
	}

	// SettingsSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.cs.session.datum";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(2);
		results.add(new BasicTextFieldSettingSpecifier("sourceIdTemplate", DEFAULT_SOURCE_ID_TEMPLATE));
		results.add(new BasicTextFieldSettingSpecifier("maxTemperatureScale",
				String.valueOf(DEFAULT_MAX_TEMPERATURE_SCALE)));
		results.add(new BasicTextFieldSettingSpecifier("purgePostedChargeSessionsExpirationHours",
				String.valueOf(PurgePostedChargeSessionsTask.DEFAULT_EXPIRATION_HOURS)));
		return results;
	}

	/**
	 * Get the source ID template.
	 * 
	 * @return the template; defaults to {@link #DEFAULT_SOURCE_ID_TEMPLATE}
	 */
	public String getSourceIdTemplate() {
		return sourceIdTemplate;
	}

	/**
	 * Set the source ID template.
	 * 
	 * <p>
	 * This template string allows for these parameters:
	 * </p>
	 * 
	 * <ol>
	 * <li><code>{chargePointId}</code> - the Charge Point ID (number)</li>
	 * <li><code>{chargerIdentifier}</code> - the Charge Point info identifier
	 * (string)</li>
	 * <li><code>{connectorId}</code> - the connector ID (integer)</li>
	 * <li><code>{location}</code> - the location (string)</li>
	 * <li><code>{phase}</code> - the phase (string)</li>
	 * </ol>
	 * 
	 * @param sourceIdTemplate
	 *        the template to set
	 */
	public void setSourceIdTemplate(String sourceIdTemplate) {
		this.sourceIdTemplate = sourceIdTemplate;
	}

	/**
	 * Get the maximum temperature decimal scale.
	 * 
	 * @return the maximum scale; defaults to
	 *         {@link #DEFAULT_MAX_TEMPERATURE_SCALE}
	 */
	public int getMaxTemperatureScale() {
		return maxTemperatureScale;
	}

	/**
	 * Set the maximum temperature decimal scale.
	 * 
	 * <p>
	 * This sets the maximum number of decimal digits for normalized temperature
	 * values. Set to {@literal -1} for no maximum.
	 * </p>
	 * 
	 * @param maxTemperatureScale
	 *        the maximum scale to set
	 */
	public void setMaxTemperatureScale(int maxTemperatureScale) {
		this.maxTemperatureScale = maxTemperatureScale;
	}

	/**
	 * Get the task scheduler.
	 * 
	 * @return the task scheduler
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler.
	 * 
	 * @param taskScheduler
	 *        the task scheduler to set
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get the number of hours after which posted charge sessions may be purged
	 * (deleted).
	 * 
	 * @return the posted charge sessions expiration time, in hours
	 */
	public int getPurgePostedChargeSessionsExpirationHours() {
		return purgePostedTask.getExpirationHours();
	}

	/**
	 * Set the number of hours after which posted charge sessions may be purged
	 * (deleted).
	 * 
	 * @param hours
	 *        posted charge sessions expiration time, in hours
	 */
	public void setPurgePostedChargeSessionsExpirationHours(int hours) {
		purgePostedTask.setExpirationHours(hours);
	}

}
