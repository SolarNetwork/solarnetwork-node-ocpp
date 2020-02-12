/* ==================================================================
 * StatusNotificationProcessor.java - 13/02/2020 2:53:30 am
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

package net.solarnetwork.node.ocpp.v16.cs;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.ocpp.dao.ChargePointConnectorDao;
import net.solarnetwork.node.ocpp.domain.ActionMessage;
import net.solarnetwork.node.ocpp.domain.ChargePointErrorCode;
import net.solarnetwork.node.ocpp.domain.ChargePointStatus;
import net.solarnetwork.node.ocpp.domain.StatusNotification;
import net.solarnetwork.node.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.node.ocpp.service.ActionMessageResultHandler;
import ocpp.domain.Action;
import ocpp.domain.ErrorCodeException;
import ocpp.v16.ActionErrorCode;
import ocpp.v16.CentralSystemAction;
import ocpp.v16.cs.StatusNotificationRequest;
import ocpp.v16.cs.StatusNotificationResponse;

/**
 * Process {@link StatusNotificationRequest} action messages.
 * 
 * @author matt
 * @version 1.0
 */
public class StatusNotificationProcessor
		implements ActionMessageProcessor<StatusNotificationRequest, StatusNotificationResponse> {

	/** The supported actions of this processor. */
	public static final Set<Action> SUPPORTED_ACTIONS = Collections
			.singleton(CentralSystemAction.StatusNotification);

	private final ChargePointConnectorDao chargePointConnectorDao;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param chargePointConnectorDao
	 *        the DAO to persist status notifications to
	 * @throws IllegalArgumentException
	 *         if {@code chargePointConnectorDao} is {@literal null}
	 */
	public StatusNotificationProcessor(ChargePointConnectorDao chargePointConnectorDao) {
		super();
		if ( chargePointConnectorDao == null ) {
			throw new IllegalArgumentException(
					"The chargePointConnectorDao parameter must not be null.");
		}
		this.chargePointConnectorDao = chargePointConnectorDao;
	}

	@Override
	public Set<Action> getSupportedActions() {
		return SUPPORTED_ACTIONS;
	}

	@Override
	public void processActionMessage(ActionMessage<StatusNotificationRequest> message,
			ActionMessageResultHandler<StatusNotificationRequest, StatusNotificationResponse> resultHandler) {
		final String chargePointId = message.getClientId();
		final StatusNotificationRequest req = message.getMessage();
		if ( req == null || chargePointId == null ) {
			ErrorCodeException err = new ErrorCodeException(ActionErrorCode.FormationViolation,
					"Missing StatusNotificationRequest message.");
			resultHandler.handleActionMessageResult(message, null, err);
			return;
		}

		// @formatter:off
		StatusNotification info = StatusNotification.builder()
				.withConnectorId(req.getConnectorId())
				.withStatus(statusValue(req))
				.withErrorCode(errorCode(req))
				.withTimestamp(timestamp(req))
				.withInfo(req.getInfo())
				.withVendorId(req.getVendorId())
				.withVendorErrorCode(req.getVendorErrorCode())
				.build();
		// @formatter:on

		log.info("Received Charge Point {} status: {}", chargePointId, info);

		try {
			chargePointConnectorDao.saveStatusInfo(chargePointId, info);
			resultHandler.handleActionMessageResult(message, new StatusNotificationResponse(), null);
		} catch ( Throwable t ) {
			ErrorCodeException err = new ErrorCodeException(ActionErrorCode.InternalError,
					"Internal error: " + t.getMessage());
			resultHandler.handleActionMessageResult(message, null, err);
		}
	}

	private ChargePointStatus statusValue(StatusNotificationRequest req) {
		if ( req != null && req.getStatus() != null ) {
			try {
				return ChargePointStatus.valueOf(req.getStatus().value());
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return ChargePointStatus.Unknown;
	}

	private ChargePointErrorCode errorCode(StatusNotificationRequest req) {
		if ( req != null && req.getStatus() != null ) {
			try {
				return ChargePointErrorCode.valueOf(req.getErrorCode().value());
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return ChargePointErrorCode.Unknown;
	}

	private Instant timestamp(StatusNotificationRequest req) {
		return req.getTimestamp() != null
				? Instant.ofEpochMilli(req.getTimestamp().toGregorianCalendar().getTimeInMillis())
				: Instant.now();
	}

}
