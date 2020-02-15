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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.ocpp.dao.ChargeSessionDao;
import net.solarnetwork.node.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.node.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.node.ocpp.domain.ChargeSession;
import net.solarnetwork.node.ocpp.domain.ChargeSessionEndInfo;
import net.solarnetwork.node.ocpp.domain.ChargeSessionStartInfo;
import net.solarnetwork.node.ocpp.domain.Location;
import net.solarnetwork.node.ocpp.domain.Measurand;
import net.solarnetwork.node.ocpp.domain.Phase;
import net.solarnetwork.node.ocpp.domain.ReadingContext;
import net.solarnetwork.node.ocpp.domain.SampledValue;
import net.solarnetwork.node.ocpp.domain.UnitOfMeasure;
import net.solarnetwork.node.ocpp.service.AuthorizationException;
import net.solarnetwork.node.ocpp.service.AuthorizationService;
import net.solarnetwork.node.ocpp.service.cs.ChargeSessionManager;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * A {@link ChargeSessionManager} that generates {@link Datum
 * 
 * @author matt
 * @version 1.0
 */
public class SolarNetChargeSessionManager extends BaseIdentifiable implements ChargeSessionManager {

	/** A datum property name for a charging session ID. */
	public static final String SESSION_ID_PROPERTY = "sessionId";

	/** The default {@code sourceIdTemplate} value. */
	public static final String DEFAULT_SOURCE_ID_TEMPLATE = "/ocpp/cp/{chargePointId}/{connectorId}/{location}";

	/** The default {@code maxTemperatureScale} value. */
	public static final int DEFAULT_MAX_TEMPERATURE_SCALE = 1;

	private final AuthorizationService authService;
	private final ChargeSessionDao chargeSessionDao;
	private final OptionalService<DatumDao<GeneralNodeDatum>> datumDao;
	private String sourceIdTemplate = DEFAULT_SOURCE_ID_TEMPLATE;
	private int maxTemperatureScale = DEFAULT_MAX_TEMPERATURE_SCALE;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param authService
	 *        the authorization service to use
	 * @param chargeSessionDao
	 *        the charge session DAO to use
	 * @param datumDao
	 *        the DAO for saving Datum
	 */
	public SolarNetChargeSessionManager(AuthorizationService authService,
			ChargeSessionDao chargeSessionDao, OptionalService<DatumDao<GeneralNodeDatum>> datumDao) {
		super();
		this.authService = authService;
		this.chargeSessionDao = chargeSessionDao;
		this.datumDao = datumDao;
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

		// check for existing session, e.g. ConcurrentTx
		ChargeSession sess = chargeSessionDao
				.getIncompleteChargeSessionForConnector(info.getChargePointId(), info.getConnectorId());
		if ( sess != null ) {
			throw new AuthorizationException(
					String.format("ChargeSession %s already active for Charge Point %s connector %d",
							sess.getId(), info.getChargePointId(), info.getConnectorId()),
					new AuthorizationInfo(info.getAuthorizationId(), AuthorizationStatus.ConcurrentTx));
		}

		// persist a new session and then re-load to get the generated transaction ID
		try {
			sess = new ChargeSession(UUID.randomUUID(), info.getTimestampStart(),
					info.getAuthorizationId(), info.getChargePointId(), info.getConnectorId(), 0);
			sess = chargeSessionDao.get(chargeSessionDao.save(sess));
		} catch ( DataIntegrityViolationException e ) {
			// assume this is from no matching Charge Point for the given chargePointId value
			throw new AuthorizationException(new AuthorizationInfo(info.getAuthorizationId(),
					AuthorizationStatus.Invalid, null, null));
		}

		// generate Datum from start meter value

		// generate reading from end meter value

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
			GeneralNodeDatum d = datum(sess, reading);
			if ( d != null ) {
				dao.storeDatum(d);
			}
		}

		return sess;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public ChargeSession getActiveChargingSession(String chargePointId, int transactionId)
			throws AuthorizationException {
		return chargeSessionDao.getIncompleteChargeSessionForTransaction(chargePointId, transactionId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AuthorizationInfo endChargingSession(ChargeSessionEndInfo info) {
		ChargeSession sess = chargeSessionDao.getIncompleteChargeSessionForTransaction(
				info.getChargePointId(), info.getTransactionId());
		if ( sess == null ) {
			throw new AuthorizationException("No active charging session found.", new AuthorizationInfo(
					info.getAuthorizationId(), AuthorizationStatus.Invalid, null, null));
		}

		sess.setEndAuthId(info.getAuthorizationId());
		sess.setEnded(info.getTimestampEnd());
		sess.setEndReason(info.getReason());
		chargeSessionDao.save(sess);

		// generate reading from end meter value

		// @formatter:off
		SampledValue reading = SampledValue.builder()
				.withSessionId(sess.getId())
				.withTimestamp(sess.getCreated())
				.withContext(ReadingContext.TransactionEnd)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue(String.valueOf(info.getMeterEnd()))
				.build();
		// @formatter:on
		chargeSessionDao.addReadings(singleton(reading));
		DatumDao<GeneralNodeDatum> dao = datumDao.service();
		if ( dao != null ) {
			GeneralNodeDatum d = datum(sess, reading);
			if ( d != null ) {
				dao.storeDatum(d);
			}
		}

		return new AuthorizationInfo(info.getAuthorizationId(), AuthorizationStatus.Accepted, null,
				null);
	}

	private GeneralNodeDatum datum(ChargeSession sess, SampledValue reading) {
		GeneralNodeDatum d = new GeneralNodeDatum();
		populateProperty(d, reading.getMeasurand(), reading.getUnit(), reading.getValue());
		if ( d.getSamples() != null && !d.getSamples().isEmpty() ) {
			d.setCreated(new Date(sess.getCreated().toEpochMilli()));
			d.setSourceId(sourceId(sess.getChargePointId(), sess.getConnectorId(), reading.getLocation(),
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

		Map<UUID, Set<SampledValue>> currentReadings = new HashMap<>(2);
		Map<UUID, ChargeSession> sessions = new HashMap<>(2);
		List<SampledValue> newReadings = new ArrayList<>();
		for ( SampledValue r : readings ) {
			Set<SampledValue> current = currentReadings.get(r.getSessionId());
			if ( current == null ) {
				ChargeSession sess = chargeSessionDao.get(r.getSessionId());
				if ( sess == null ) {
					throw new AuthorizationException("No active charging session found.",
							new AuthorizationInfo(null, AuthorizationStatus.Invalid, null, null));
				}
				sessions.put(r.getSessionId(), sess);
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
				for ( SampledValue reading : newReadings ) {
					GeneralNodeDatum d = datum(sessions.get(reading.getSessionId()), reading);
					if ( d != null ) {
						dao.storeDatum(d);
					}
				}
			}
		}
	}

	private String sourceId(String chargePointId, int connectorId, Location location, Phase phase) {
		Map<String, Object> params = new HashMap<>(4);
		params.put("chargePointId", chargePointId);
		params.put("connectorId", connectorId);
		params.put("location", location);
		params.put("phase", phase);
		return StringUtils.expandTemplateString(sourceIdTemplate, params);
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
	 * <li><code>{chargePointId}</code> - the Charge Point ID (string)</li>
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

}
