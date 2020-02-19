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
import java.util.concurrent.CompletableFuture;
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
import net.solarnetwork.node.ocpp.dao.AuthorizationDao;
import net.solarnetwork.node.ocpp.dao.ChargePointConnectorDao;
import net.solarnetwork.node.ocpp.dao.ChargePointDao;
import net.solarnetwork.node.ocpp.domain.ActionMessage;
import net.solarnetwork.node.ocpp.domain.Authorization;
import net.solarnetwork.node.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.node.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.node.ocpp.domain.BasicActionMessage;
import net.solarnetwork.node.ocpp.domain.ChargePoint;
import net.solarnetwork.node.ocpp.domain.ChargePointConnector;
import net.solarnetwork.node.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.node.ocpp.domain.ChargePointInfo;
import net.solarnetwork.node.ocpp.domain.ChargePointStatus;
import net.solarnetwork.node.ocpp.domain.RegistrationStatus;
import net.solarnetwork.node.ocpp.domain.StatusNotification;
import net.solarnetwork.node.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.node.ocpp.service.AuthorizationService;
import net.solarnetwork.node.ocpp.service.ChargePointBroker;
import net.solarnetwork.node.ocpp.service.ChargePointRouter;
import net.solarnetwork.node.ocpp.service.cs.ChargePointManager;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import ocpp.domain.Action;
import ocpp.domain.ErrorCodeException;
import ocpp.v16.ActionErrorCode;
import ocpp.v16.ChargePointAction;
import ocpp.v16.ConfigurationKey;
import ocpp.v16.cp.AvailabilityStatus;
import ocpp.v16.cp.AvailabilityType;
import ocpp.v16.cp.ChangeAvailabilityRequest;
import ocpp.v16.cp.ChangeAvailabilityResponse;
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

	@Override
	public Set<String> availableChargePointsIds() {
		return chargePointRouter.availableChargePointsIds();
	}

	@Override
	public boolean isChargePointAvailable(String chargePointId) {
		return chargePointRouter.availableChargePointsIds().contains(chargePointId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChargePoint registerChargePoint(ChargePointInfo info) {
		log.info("Charge Point registration received: {}", info);

		if ( info == null || info.getId() == null ) {
			throw new IllegalArgumentException("The ChargePoint ID must be provided.");
		}

		ChargePoint cp = chargePointDao.get(info.getId());
		if ( cp == null ) {
			cp = registerNewChargePoint(info);
		} else if ( cp.isEnabled() ) {
			cp = updateChargePointInfo(cp, info);
		}

		sendToChargePoint(info.getId(), ChargePointAction.GetConfiguration,
				new GetConfigurationRequest(), processConfiguration(info.getId()));

		return cp;
	}

	private ChargePoint registerNewChargePoint(ChargePointInfo info) {
		log.info("Registering new ChargePoint {}", info);
		ChargePoint cp = new ChargePoint(info.getId(), Instant.now());
		cp.setEnabled(true);
		cp.setRegistrationStatus(getInitialRegistrationStatus());
		cp.setInfo(info);
		chargePointDao.save(cp);
		return cp;
	}

	private ChargePoint updateChargePointInfo(ChargePoint cp, ChargePointInfo info) {
		assert cp != null && cp.getInfo() != null;
		if ( cp.getInfo().isSameAs(info) ) {
			log.info("ChargePoint registration info is unchanged: {}", info);
		} else {
			log.info("Updating ChargePoint registration info {} -> {}", cp.getInfo(), info);
			cp.setInfo(info);
			chargePointDao.save(cp);
		}
		return cp;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean isChargePointRegistrationAccepted(String chargePointId) {
		ChargePoint cp = chargePointDao.get(chargePointId);
		return cp != null && cp.isEnabled() && cp.getRegistrationStatus() == RegistrationStatus.Accepted;
	}

	private ActionMessageResultHandler<GetConfigurationRequest, GetConfigurationResponse> processConfiguration(
			String chargePointId) {
		return (msg, confs, err) -> {
			if ( confs != null && confs.getConfigurationKey() != null
					&& !confs.getConfigurationKey().isEmpty() ) {
				tryWithTransaction(new TransactionCallbackWithoutResult() {

					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						ChargePoint cp = chargePointDao.get(chargePointId);
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
								.findByIdChargePointId(cp.getId());
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
				log.warn("Error requesting configuration from charge point {}: {}", chargePointId,
						err.getMessage());
			}
			return true;
		};
	}

	@Override
	public CompletableFuture<Boolean> adjustConnectorEnabledState(String chargePointId, int connectorId,
			boolean enabled) {
		ChangeAvailabilityRequest req = new ChangeAvailabilityRequest();
		req.setConnectorId(connectorId);
		req.setType(enabled ? AvailabilityType.OPERATIVE : AvailabilityType.INOPERATIVE);
		CompletableFuture<Boolean> result = new CompletableFuture<>();
		sendToChargePoint(chargePointId, ChargePointAction.ChangeAvailability, req,
				changeAvailability(chargePointId, req, result));
		return result;
	}

	private ActionMessageResultHandler<ChangeAvailabilityRequest, ChangeAvailabilityResponse> changeAvailability(
			String chargePointId, ChangeAvailabilityRequest req, CompletableFuture<Boolean> future) {
		return (msg, res, err) -> {
			if ( res != null ) {
				AvailabilityStatus status = res.getStatus();
				if ( status == AvailabilityStatus.ACCEPTED ) {
					log.info("Charge Point {} connector {} availability set to {}", chargePointId,
							req.getConnectorId(), req.getType());
					try {
						chargePointConnectorDao.updateChargePointStatus(chargePointId,
								req.getConnectorId(),
								req.getType() == AvailabilityType.OPERATIVE ? ChargePointStatus.Available
										: ChargePointStatus.Unavailable);
					} catch ( RuntimeException e ) {
						log.error("Error saving Charge Point {} connector {} status {}: {}",
								chargePointId, req.getConnectorId(), e.toString());
					}
					future.complete(true);
				} else {
					log.warn("Charge Point {} connector {} availability rejected change to {}",
							chargePointId, req.getConnectorId(), req.getType());
					future.complete(false);
				}
			} else {
				future.completeExceptionally(err);
			}
			return true;
		};
	}

	@Override
	public AuthorizationInfo authorize(final String clientId, final String idTag) {
		Authorization auth = null;
		if ( clientId != null && idTag != null ) {
			auth = authorizationDao.get(idTag);
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

	private <T, R> void sendToChargePoint(String chargePointId, Action action, T payload,
			ActionMessageResultHandler<T, R> handler) {
		executor.execute(() -> {
			ActionMessage<T> msg = new BasicActionMessage<T>(chargePointId, UUID.randomUUID().toString(),
					action, payload);
			ChargePointBroker broker = chargePointRouter.brokerForChargePoint(chargePointId);
			if ( broker != null ) {
				if ( broker.sendMessageToChargePoint(msg, handler) ) {
					return;
				}
			} else {
				log.warn("No ChargePointBroker available for client {}", chargePointId);
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

		Set<String> availableChargePointIds;
		try {
			availableChargePointIds = availableChargePointsIds();
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
			cpSettings.add(new BasicTitleSettingSpecifier(cp.getId(),
					chargePointStatus(cp, availableChargePointIds), true));
		}
		results.add(new BasicGroupSettingSpecifier("chargePoints", cpSettings));

		return results;
	}

	private String chargePointStatus(ChargePoint cp, Set<String> availableChargePointIds) {
		StringBuilder buf = new StringBuilder();
		buf.append(availableChargePointIds.contains(cp.getId())
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
