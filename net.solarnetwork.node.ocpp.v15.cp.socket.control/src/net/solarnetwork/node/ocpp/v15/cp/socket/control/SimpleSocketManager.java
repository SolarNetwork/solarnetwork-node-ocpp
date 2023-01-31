/* ==================================================================
 * SimpleSocketManager.java - 31/07/2016 7:34:18 AM
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

package net.solarnetwork.node.ocpp.v15.cp.socket.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.InstructionStatus;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.ocpp.v15.cp.ChargeSession;
import net.solarnetwork.node.ocpp.v15.cp.ChargeSessionManager;
import net.solarnetwork.node.ocpp.v15.cp.OCPPException;
import net.solarnetwork.node.ocpp.v15.cp.SocketManager;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.Identifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Implementation of {@link SocketManager} that uses the
 * {@link InstructionHandler#TOPIC_SET_CONTROL_PARAMETER} instruction to turn
 * sockets on/off.
 * 
 * The {@link Identifiable} properties of this class are delegated to the
 * configured {@link ChargeSessionManager}.
 * 
 * @author matt
 * @version 2.0
 */
public class SimpleSocketManager implements SocketManager, SettingSpecifierProvider {

	private OptionalService<InstructionExecutionService> instructionService;
	private OptionalService<EventAdmin> eventAdmin;

	private ChargeSessionManager chargeSessionManager;
	private int chargeSessionExpirationMinutes = 6 * 60;
	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public boolean adjustSocketEnabledState(String socketId, boolean enabled) {
		InstructionExecutionService service = OptionalService.service(instructionService);
		if ( service == null ) {
			return false;
		}
		final Instruction instr = InstructionUtils.createSetControlValueLocalInstruction(socketId,
				String.valueOf(enabled));
		log.debug("Requesting socket {} to be {}", socketId, enabled ? "enabled" : "disabled");
		InstructionStatus result = service.executeInstruction(instr);
		log.debug("Request for socket {} to be {} resulted in {}", socketId,
				enabled ? "enabled" : "disabled", result);
		if ( result == null ) {
			return false;
		}
		if ( result.getInstructionState() == InstructionStatus.InstructionState.Completed ) {
			String eventTopic = (enabled ? ChargeSessionManager.EVENT_TOPIC_SOCKET_ACTIVATED
					: ChargeSessionManager.EVENT_TOPIC_SOCKET_DEACTIVATED);
			Map<String, Object> eventProps = Collections
					.singletonMap(ChargeSessionManager.EVENT_PROPERTY_SOCKET_ID, (Object) socketId);
			postEvent(eventTopic, eventProps);
		}
		return (result.getInstructionState() != InstructionState.Declined);
	}

	@Override
	public String getUid() {
		return (chargeSessionManager != null ? chargeSessionManager.getUid() : null);
	}

	@Override
	public String getGroupUid() {
		return (chargeSessionManager != null ? chargeSessionManager.getGroupUid() : null);
	}

	private void postEvent(String topic, Map<String, Object> props) {
		OptionalService<EventAdmin> eaService = eventAdmin;
		EventAdmin ea = (eaService == null ? null : eaService.service());
		if ( ea == null ) {
			return;
		}
		log.debug("Posting event {}: {}", topic, props);
		Event event = new Event(topic, props);
		ea.postEvent(event);
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.ocpp.v15.cp.socket.control";
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
		SimpleSocketManager defaults = new SimpleSocketManager();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(1);
		results.add(new BasicTextFieldSettingSpecifier(
				"filterableChargeSessionManager.propertyFilters['UID']", "OCPP Central System"));
		results.add(new BasicTextFieldSettingSpecifier("chargeSessionExpirationMinutes",
				String.valueOf(defaults.chargeSessionExpirationMinutes)));
		return results;
	}

	/**
	 * Find all active charge sessions and make sure the sockets associated with
	 * those sessions are enabled. This should be called when the service is
	 * first initialized, to make sure the socket state is synchronized with
	 * charge session state.
	 */
	public void verifyAllSockets() {
		Collection<String> availableSockets = chargeSessionManager.availableSocketIds();
		for ( String socketId : availableSockets ) {
			ChargeSession session = chargeSessionManager.activeChargeSession(socketId);
			boolean expired = chargeSessionExpired(session);
			if ( expired ) {
				log.info("OCPP charge session {} for IdTag {} has expired", session.getSessionId(),
						session.getIdTag());
				try {
					chargeSessionManager.completeChargeSession(session.getIdTag(),
							session.getSessionId());
				} catch ( OCPPException e ) {
					log.warn("Error completing expired OCPP session: {}", e.getMessage());
				}
			}
			boolean enabled = (session != null && !expired);
			if ( !adjustSocketEnabledState(socketId, enabled) ) {
				log.warn("Unable to adjust socket {} enabled state to {}", socketId,
						enabled ? "enabled" : "disabled");
			}
		}
	}

	/**
	 * Test if a session has expired.
	 * 
	 * @param session
	 *        The session, or <em>null</em>.
	 * @return <em>true</em> if {@code session} is non-null and the current time
	 *         is greater than {@link ChargeSession#getExpiryDate()} or the
	 *         {@link ChargeSession#getCreated()} plus
	 *         {@code chargeSessionExpirationMinutes}.
	 */
	private boolean chargeSessionExpired(ChargeSession session) {
		if ( session == null ) {
			return false;
		}
		final long now = System.currentTimeMillis();
		if ( session.getExpiryDate() != null ) {
			long expireTime = session.getExpiryDate().toGregorianCalendar().getTimeInMillis();
			if ( expireTime < now ) {
				return true;
			}
		}
		if ( session.getCreated() != null ) {
			long expireTime = session.getCreated().getTime()
					+ (chargeSessionExpirationMinutes * 60 * 1000L);
			if ( expireTime < now ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Set the {@link EventAdmin} to use for posting socket state change events.
	 * 
	 * The {@link ChargeSessionManager#EVENT_TOPIC_SOCKET_ACTIVATED} and
	 * {@link ChargeSessionManager#EVENT_TOPIC_SOCKET_DEACTIVATED} events will
	 * be sent if configured.
	 * 
	 * @param eventAdmin
	 *        The service to use.
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Set the instruction execution service.
	 * 
	 * @param service
	 *        the service to use
	 */
	public void setInstructionService(OptionalService<InstructionExecutionService> service) {
		this.instructionService = service;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setChargeSessionManager(ChargeSessionManager chargeSessionManager) {
		this.chargeSessionManager = chargeSessionManager;
	}

	/**
	 * Get the minimum number of minutes a charge session is allowed to go
	 * before expiring.
	 * 
	 * @return The configured number of minutes.
	 */
	public int getChargeSessionExpirationMinutes() {
		return chargeSessionExpirationMinutes;
	}

	/**
	 * Set the minimum number of minutes a charge session is allowed to go
	 * before expiring.
	 * 
	 * @param chargeSessionExpirationMinutes
	 *        The minimum number of minutes before a charge session is
	 *        considered expired. Defaults to {@code 360} (6 hours).
	 */
	public void setChargeSessionExpirationMinutes(int chargeSessionExpirationMinutes) {
		if ( chargeSessionExpirationMinutes < 0 ) {
			chargeSessionExpirationMinutes = 0;
		}
		this.chargeSessionExpirationMinutes = chargeSessionExpirationMinutes;
	}

	/**
	 * Get the {@link ChargeSessionManager} as a {@link FilterableService}.
	 * 
	 * @return The filterable {@link ChargeSessionManager}, or <em>null</em> if
	 *         it is not filterable.
	 */
	public FilterableService getFilterableChargeSessionManager() {
		ChargeSessionManager mgr = chargeSessionManager;
		if ( mgr instanceof FilterableService ) {
			return (FilterableService) mgr;
		}
		return null;
	}

}
