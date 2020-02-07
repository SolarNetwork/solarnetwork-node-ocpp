/* ==================================================================
 * BootNotificationProcessor.java - 6/02/2020 5:08:50 pm
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

package net.solarnetwork.node.ocpp.v16.cs.broker;

import java.util.Collections;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.ocpp.domain.ActionMessage;
import net.solarnetwork.node.ocpp.domain.ChargePoint;
import net.solarnetwork.node.ocpp.domain.ChargePointInfo;
import net.solarnetwork.node.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.node.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.node.ocpp.service.cs.ChargePointManager;
import net.solarnetwork.util.OptionalServiceCollection;
import ocpp.domain.Action;
import ocpp.domain.ErrorCodeException;
import ocpp.v16.ActionErrorCode;
import ocpp.v16.CentralSystemAction;
import ocpp.v16.cs.BootNotificationRequest;
import ocpp.v16.cs.BootNotificationResponse;
import ocpp.v16.cs.RegistrationStatus;
import ocpp.xml.support.XmlDateUtils;

/**
 * Process {@link BootNotificationRequest} action messages.
 * 
 * @author matt
 * @version 1.0
 */
public class BootNotificationProcessor
		implements ActionMessageProcessor<BootNotificationRequest, BootNotificationResponse> {

	/** The supported actions of this processor. */
	public static final Set<Action> SUPPORTED_ACTIONS = Collections
			.singleton(CentralSystemAction.BootNotification);

	/** The default {@code heartbeatIntervalSeconds} value. */
	public static final int DEFAULT_HEARTBEAT_INTERVAL_SECONDS = 300;

	private final OptionalServiceCollection<ChargePointManager> chargePointManagers;
	private int heartbeatIntervalSeconds = DEFAULT_HEARTBEAT_INTERVAL_SECONDS;;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param chargePointManagers
	 *        the {@link ChargePointManager} collection to notify
	 * @throws IllegalArgumentException
	 *         if {@code chargePointManagers} is {@literal null}
	 */
	public BootNotificationProcessor(OptionalServiceCollection<ChargePointManager> chargePointManagers) {
		super();
		if ( chargePointManagers == null ) {
			throw new IllegalArgumentException("The chargePointManagers parameter must not be null.");
		}
		this.chargePointManagers = chargePointManagers;
	}

	@Override
	public Set<Action> getSupportedActions() {
		return SUPPORTED_ACTIONS;
	}

	@Override
	public void processActionMessage(ActionMessage<BootNotificationRequest> message,
			ActionMessageResultHandler<BootNotificationRequest, BootNotificationResponse> resultHandler) {
		Iterable<ChargePointManager> managers = chargePointManagers.services();
		if ( managers == null ) {
			log.warn("No ChargePointManager service(s) available to handle BootNotificationRequest {}",
					message);
			ErrorCodeException err = new ErrorCodeException(ActionErrorCode.InternalError,
					"No ChargePointManager service available.");
			resultHandler.handleActionMessageResult(message, null, err);
			return;
		}

		BootNotificationRequest req = message.getMessage();
		if ( req == null ) {
			ErrorCodeException err = new ErrorCodeException(ActionErrorCode.FormationViolation,
					"Missing BootNotificationRequest message.");
			resultHandler.handleActionMessageResult(message, null, err);
			return;
		}

		ChargePointInfo info = new ChargePointInfo(message.getClientId());
		info.setChargePointVendor(req.getChargePointVendor());
		info.setChargePointModel(req.getChargePointModel());
		info.setChargeBoxSerialNumber(req.getChargeBoxSerialNumber());
		info.setFirmwareVersion(req.getFirmwareVersion());
		info.setIccid(req.getIccid());
		info.setImsi(req.getImsi());
		info.setMeterType(req.getMeterType());
		info.setMeterSerialNumber(req.getMeterSerialNumber());

		try {
			for ( ChargePointManager mgr : managers ) {
				ChargePoint cp = mgr.registerChargePoint(info);
				if ( cp != null ) {
					BootNotificationResponse res = new BootNotificationResponse();
					res.setCurrentTime(XmlDateUtils.newXmlCalendar());
					if ( cp.getRegistrationStatus() != null ) {
						switch (cp.getRegistrationStatus()) {
							case Accepted:
								res.setStatus(RegistrationStatus.ACCEPTED);
								break;

							case Rejected:
								res.setStatus(RegistrationStatus.REJECTED);
								break;

							default:
								res.setStatus(RegistrationStatus.PENDING);
								break;
						}
					} else {
						res.setStatus(RegistrationStatus.PENDING);
					}
					res.setInterval(getHeartbeatIntervalSeconds());
					resultHandler.handleActionMessageResult(message, res, null);
					return;
				} else {
					log.debug(
							"ChargePointManager {} did not provide a ChargePoint for response to BootNotificationRequest");
				}
			}
		} catch ( Throwable t ) {
			ErrorCodeException err = new ErrorCodeException(ActionErrorCode.InternalError,
					"Internal error: " + t.getMessage());
			resultHandler.handleActionMessageResult(message, null, err);
		}

		ErrorCodeException err = new ErrorCodeException(ActionErrorCode.InternalError,
				"No ChargePointManager service handled BootNotificationRequest.");
		resultHandler.handleActionMessageResult(message, null, err);
	}

	/**
	 * Get the heartbeat interval.
	 * 
	 * @return the interval, in seconds
	 */
	public int getHeartbeatIntervalSeconds() {
		return heartbeatIntervalSeconds;
	}

	/**
	 * Set the heartbeat interval.
	 * 
	 * @param heartbeatIntervalSeconds
	 *        the interval to set, in seconds
	 */
	public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
		this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
	}

}
