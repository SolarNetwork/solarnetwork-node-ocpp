/* ==================================================================
 * OcppControllerService.java - 6/02/2020 5:18:34 pm
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

package net.solarnetwork.node.ocpp.v16.cs.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.ocpp.dao.AuthorizationDao;
import net.solarnetwork.ocpp.dao.ChargePointConnectorDao;
import net.solarnetwork.ocpp.dao.ChargePointDao;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.domain.Authorization;
import net.solarnetwork.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.ocpp.domain.BasicActionMessage;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointConnector;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.RegistrationStatus;
import net.solarnetwork.ocpp.domain.StatusNotification;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.ocpp.service.AuthorizationService;
import net.solarnetwork.ocpp.service.ChargePointBroker;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import net.solarnetwork.ocpp.service.cs.ChargePointManager;
import net.solarnetwork.security.AuthorizationException;
import net.solarnetwork.security.AuthorizationException.Reason;
import ocpp.domain.Action;
import ocpp.domain.ErrorCodeException;
import ocpp.v16.ActionErrorCode;
import ocpp.v16.ChargePointAction;
import ocpp.v16.ConfigurationKey;
import ocpp.v16.cp.GetConfigurationRequest;
import ocpp.v16.cp.GetConfigurationResponse;
import ocpp.v16.cp.KeyValue;

/**
 * API for an OCPP v1.6 local controller service.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppControllerService extends BaseIdentifiable
		implements ChargePointManager, AuthorizationService, SettingSpecifierProvider {

	/** The default {@code initialRegistrationStatus} value. */
	public static final RegistrationStatus DEFAULT_INITIAL_REGISTRATION_STATUS = RegistrationStatus.Pending;

	private final Executor executor;
	private final ChargePointRouter chargePointRouter;
	private final AuthorizationDao authorizationDao;
	private final ChargePointDao chargePointDao;
	private final ChargePointConnectorDao chargePointConnectorDao;
	private RegistrationStatus initialRegistrationStatus;
	private TransactionTemplate transactionTemplate;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param executor
	 *        a task runner
	 * @param chargePointRouter
	 *        the broker router to push messages to Charge Points with with
	 * @param authorizationDao
	 *        the {@link Authorization} DAO to use
	 * @param chargePointDao
	 *        the {@link ChargePoint} DAO to use
	 * @param chargePointConnectorDao
	 *        the {@link ChargePointConnector} DAO to use
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public OcppControllerService(Executor executor, ChargePointRouter chargePointRouter,
			AuthorizationDao authorizationDao, ChargePointDao chargePointDao,
			ChargePointConnectorDao chargePointConnectorDao) {
		super();
		if ( executor == null ) {
			throw new IllegalArgumentException("The executor parameter must not be null.");
		}
		this.executor = executor;
		if ( chargePointRouter == null ) {
			throw new IllegalArgumentException("The chargePointRouter parameter must not be null.");
		}
		this.chargePointRouter = chargePointRouter;
		if ( authorizationDao == null ) {
			throw new IllegalArgumentException("The authorizationDao parameter must not be null.");
		}
		this.authorizationDao = authorizationDao;
		if ( chargePointDao == null ) {
			throw new IllegalArgumentException("The chargePointDao parameter must not be null.");
		}
		this.chargePointDao = chargePointDao;
		if ( chargePointConnectorDao == null ) {
			throw new IllegalArgumentException(
					"The chargePointConnectorDao parameter must not be null.");
		}
		this.chargePointConnectorDao = chargePointConnectorDao;
		this.initialRegistrationStatus = DEFAULT_INITIAL_REGISTRATION_STATUS;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChargePoint registerChargePoint(ChargePointIdentity identity, ChargePointInfo info) {
		log.info("Charge Point registration received: {}", info);

		if ( info == null || info.getId() == null ) {
			throw new IllegalArgumentException("The ChargePoint ID must be provided.");
		}

		ChargePoint cp = chargePointDao.getForIdentity(identity);
		if ( cp == null ) {
			cp = registerNewChargePoint(info);
		} else if ( cp.isEnabled() ) {
			cp = updateChargePointInfo(cp, info);
		}

		sendToChargePoint(identity, ChargePointAction.GetConfiguration, new GetConfigurationRequest(),
				processConfiguration(cp));

		return cp;
	}

	private ChargePoint registerNewChargePoint(ChargePointInfo info) {
		log.info("Registering new ChargePoint {}", info);
		ChargePoint cp = new ChargePoint(null, Instant.now(), info);
		cp.setEnabled(true);
		cp.setRegistrationStatus(getInitialRegistrationStatus());
		return chargePointDao.get(chargePointDao.save(cp));
	}

	private ChargePoint updateChargePointInfo(ChargePoint cp, ChargePointInfo info) {
		assert cp != null && cp.getInfo() != null;
		if ( cp.getInfo().isSameAs(info) ) {
			log.info("ChargePoint registration info is unchanged: {}", info);
		} else {
			log.info("Updating ChargePoint registration info {} -> {}", cp.getInfo(), info);
			cp.copyInfoFrom(info);
			chargePointDao.save(cp);
		}
		return cp;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean isChargePointRegistrationAccepted(long chargePointId) {
		ChargePoint cp = chargePointDao.get(chargePointId);
		return cp != null && cp.isEnabled() && cp.getRegistrationStatus() == RegistrationStatus.Accepted;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateChargePointStatus(ChargePointIdentity identity, StatusNotification info) {
		final ChargePoint chargePoint = chargePointDao.getForIdentity(identity);
		if ( chargePoint == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, identity);
		}
		log.info("Received Charge Point {} status: {}", identity, info);
		if ( info.getConnectorId() == 0 ) {
			chargePointConnectorDao.updateChargePointStatus(chargePoint.getId(), info.getConnectorId(),
					info.getStatus());
		} else {
			chargePointConnectorDao.saveStatusInfo(chargePoint.getId(), info);
		}
	}

	private ActionMessageResultHandler<GetConfigurationRequest, GetConfigurationResponse> processConfiguration(
			ChargePoint chargePoint) {
		return (msg, confs, err) -> {
			if ( confs != null && confs.getConfigurationKey() != null
					&& !confs.getConfigurationKey().isEmpty() ) {
				tryWithTransaction(new TransactionCallbackWithoutResult() {

					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						ChargePoint cp = chargePointDao.get(chargePoint.getId());
						ChargePoint orig = new ChargePoint(cp);
						KeyValue numConnsKey = confs.getConfigurationKey().stream()
								.filter(k -> ConfigurationKey.NumberOfConnectors.getName()
										.equalsIgnoreCase(k.getKey()) && k.getValue() != null)
								.findAny().orElse(null);
						if ( numConnsKey != null ) {
							try {
								cp.setConnectorCount(Integer.parseInt(numConnsKey.getValue()));
							} catch ( NumberFormatException e ) {
								log.error("{} key invalid integer value: [{}]",
										ConfigurationKey.NumberOfConnectors, numConnsKey.getValue());
							}
						}
						if ( !cp.isSameAs(orig) ) {
							chargePointDao.save(cp);
							log.info("Saved configuration changes to Charge Point {}", cp.getId());
						}

						// add missing ChargePointConnector entities; remove excess
						Collection<ChargePointConnector> connectors = chargePointConnectorDao
								.findByChargePointId(cp.getId());
						Map<Integer, ChargePointConnector> existing = connectors.stream().collect(
								Collectors.toMap(cpc -> cpc.getId().getConnectorId(), cpc -> cpc));
						for ( int i = 1; i <= cp.getConnectorCount(); i++ ) {
							if ( !existing.containsKey(i) ) {
								ChargePointConnector conn = new ChargePointConnector(
										new ChargePointConnectorKey(cp.getId(), i), Instant.now());
								conn.setInfo(StatusNotification.builder().withConnectorId(i)
										.withTimestamp(conn.getCreated()).build());
								log.info("Creating ChargePointConnector {} for Charge Point {}", i,
										cp.getId());
								chargePointConnectorDao.save(conn);
							}
						}
						for ( Iterator<Entry<Integer, ChargePointConnector>> itr = existing.entrySet()
								.iterator(); itr.hasNext(); ) {
							Entry<Integer, ChargePointConnector> e = itr.next();
							int connId = e.getKey().intValue();
							if ( connId < 1 || connId > cp.getConnectorCount() ) {
								log.info("Deleting excess ChargePointConnector {} from Charge Point {}",
										connId, cp.getId());
								chargePointConnectorDao.delete(e.getValue());
								itr.remove();
							}
						}
					}
				});
			} else if ( err != null ) {
				log.warn("Error requesting configuration from charge point {}: {}",
						chargePoint.getInfo().getId(), err.getMessage());
			}
			return true;
		};
	}

	@Override
	public AuthorizationInfo authorize(final ChargePointIdentity clientId, final String idTag) {
		Authorization auth = null;
		if ( clientId != null && idTag != null ) {
			auth = authorizationDao.getForToken(idTag);
		}
		AuthorizationInfo.Builder result = AuthorizationInfo.builder().withId(idTag);
		if ( auth != null ) {
			result.withExpiryDate(auth.getExpiryDate()).withParentId(auth.getParentId());
			if ( !auth.isEnabled() ) {
				result.withStatus(AuthorizationStatus.Blocked);
			} else if ( auth.isExpired() ) {
				result.withStatus(AuthorizationStatus.Expired);
			} else {
				result.withStatus(AuthorizationStatus.Accepted);
			}
		} else {
			result.withStatus(AuthorizationStatus.Invalid);
		}
		return result.build();
	}

	private <T> T tryWithTransaction(TransactionCallback<T> tx) {
		final TransactionTemplate tt = getTransactionTemplate();
		if ( tt != null ) {
			return tt.execute(tx);
		} else {
			return tx.doInTransaction(null);
		}
	}

	private <T, R> void sendToChargePoint(ChargePointIdentity identity, Action action, T payload,
			ActionMessageResultHandler<T, R> handler) {
		executor.execute(() -> {
			ActionMessage<T> msg = new BasicActionMessage<T>(identity, UUID.randomUUID().toString(),
					action, payload);
			ChargePointBroker broker = chargePointRouter.brokerForChargePoint(identity);
			if ( broker != null ) {
				if ( broker.sendMessageToChargePoint(msg, handler) ) {
					return;
				}
			} else {
				log.warn("No ChargePointBroker available for {}", identity);
			}
			handler.handleActionMessageResult(msg, null,
					new ErrorCodeException(ActionErrorCode.GenericError, "Client not available."));
		});
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.v16.cs.controller";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);

		Set<ChargePointIdentity> availableChargePointIds;
		try {
			availableChargePointIds = chargePointRouter.availableChargePointsIds();
		} catch ( Exception e ) {
			availableChargePointIds = Collections.emptySet();
		}

		Collection<ChargePoint> chargePoints;
		try {
			chargePoints = chargePointDao.getAll(null);
		} catch ( Exception e ) {
			chargePoints = Collections.emptyList();
		}

		List<SettingSpecifier> cpSettings = new ArrayList<>(chargePoints.size());
		for ( ChargePoint cp : chargePoints ) {
			cpSettings.add(new BasicTitleSettingSpecifier(cp.getInfo().getId(),
					chargePointStatus(cp, availableChargePointIds), true));
		}
		results.add(new BasicGroupSettingSpecifier("chargePoints", cpSettings));

		return results;
	}

	private String chargePointStatus(ChargePoint cp, Set<ChargePointIdentity> availableChargePointIds) {
		StringBuilder buf = new StringBuilder();
		ChargePointIdentity identity = cp.chargePointIdentity();
		buf.append(availableChargePointIds.contains(identity)
				? getMessageSource().getMessage("connected.label", null, "Connected",
						Locale.getDefault())
				: getMessageSource().getMessage("disconnected.label", null, "Not connected",
						Locale.getDefault()));
		buf.append("; ").append(getMessageSource().getMessage("registrationStatus.label", null,
				"Registration status", Locale.getDefault())).append(": ");
		RegistrationStatus regStatus = cp.getRegistrationStatus();
		if ( regStatus == null ) {
			regStatus = RegistrationStatus.Unknown;
		}
		buf.append(getMessageSource().getMessage("registrationStatus." + regStatus.name(), null,
				regStatus.toString(), Locale.getDefault()));

		return buf.toString();
	}

	/**
	 * Get the initial {@link RegistrationStatus} to use for newly registered
	 * charge points.
	 * 
	 * @return the status, never {@literal null}
	 */
	public RegistrationStatus getInitialRegistrationStatus() {
		return initialRegistrationStatus;
	}

	/**
	 * Set the initial {@link RegistrationStatus} to use for newly registered
	 * charge points.
	 * 
	 * @param initialRegistrationStatus
	 *        the status to set
	 * @throws IllegalArgumentException
	 *         if {@code initialRegistrationStatus} is {@literal null}
	 */
	public void setInitialRegistrationStatus(RegistrationStatus initialRegistrationStatus) {
		if ( initialRegistrationStatus == null ) {
			throw new IllegalArgumentException(
					"The initialRegistrationStatus parameter must not be null.");
		}
		this.initialRegistrationStatus = initialRegistrationStatus;
	}

	/**
	 * Get the configured transaction template.
	 * 
	 * @return the transaction template
	 */
	public TransactionTemplate getTransactionTemplate() {
		return transactionTemplate;
	}

	/**
	 * Set the transaction template to use.
	 * 
	 * @param transactionTemplate
	 *        the transaction template to set
	 */
	public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

}
