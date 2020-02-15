/* ==================================================================
 * ConnectorAvailabilityControl.java - 12/02/2020 11:59:39 am
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

package net.solarnetwork.node.ocpp.control.connectors;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.domain.NodeControlInfoDatum;
import net.solarnetwork.node.ocpp.dao.ChargePointConnectorDao;
import net.solarnetwork.node.ocpp.domain.ChargePointConnector;
import net.solarnetwork.node.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.node.ocpp.domain.ChargePointStatus;
import net.solarnetwork.node.ocpp.service.cs.ChargePointManager;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.OptionalServiceCollection;
import net.solarnetwork.util.StringUtils;

/**
 * OCPP Charge Point connector control that exposes all available connectors as
 * boolean controls for the purposes of toggling their availability on/off.
 * 
 * <p>
 * This control provider uses a {@link ChargePointConnectorDao} as the
 * persistent store of available connectors to expose as boolean switch
 * controls. The generated control IDs are encoded with a Charge Point ID and
 * connector ID. When an {@link InstructionHandler#TOPIC_SET_CONTROL_PARAMETER}
 * instruction is received,
 * {@link ChargePointManager#adjustConnectorEnabledState(String, int, boolean)}
 * will be invoked to inform the Charge Point of the desired on/off state.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class ConnectorAvailabilityControl extends BaseIdentifiable
		implements EventHandler, InstructionHandler, NodeControlProvider, SettingSpecifierProvider {

	/** The default {@code controlIdTemplate} value. */
	public static final String DEFAULT_CONTROL_ID_TEMPLATE = "/ocpp/cp/{chargePointId}/{connectorId}";

	/** The default {@code controlIdRegex} value. */
	public static final Pattern DEFAULT_CONTROL_ID_REGEX = Pattern.compile("/ocpp/cp/(\\w+)/(\\d+)",
			Pattern.CASE_INSENSITIVE);

	/** The default {@code messageTimeout} value. */
	public static final long DEFAULT_MESSAGE_TIMEOUT = TimeUnit.SECONDS.toMillis(60);

	private final OptionalServiceCollection<ChargePointManager> chargePointManagers;
	private final ChargePointConnectorDao chargePointConnectorDao;
	private String controlIdTemplate;
	private Pattern controlIdRegex;
	private long messageTimeout;
	private OptionalService<EventAdmin> eventAdmin;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param chargePointManagers
	 *        the managers to use
	 * @param chargePointConnectorDao
	 *        the Charge Point connector DAO to use
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public ConnectorAvailabilityControl(
			OptionalServiceCollection<ChargePointManager> chargePointManagers,
			ChargePointConnectorDao chargePointConnectorDao) {
		super();
		if ( chargePointManagers == null ) {
			throw new IllegalArgumentException("The chargePointBroker parameter must not be null.");
		}
		this.chargePointManagers = chargePointManagers;
		if ( chargePointConnectorDao == null ) {
			throw new IllegalArgumentException(
					"The chargePointConnectorDao parameter must not be null.");
		}
		this.chargePointConnectorDao = chargePointConnectorDao;
		this.controlIdTemplate = DEFAULT_CONTROL_ID_TEMPLATE;
		this.controlIdRegex = DEFAULT_CONTROL_ID_REGEX;
		this.messageTimeout = DEFAULT_MESSAGE_TIMEOUT;
	}

	// NodeControlProvider

	@Override
	public List<String> getAvailableControlIds() {
		return chargePointConnectorDao.getAll(null).stream().map(cpc -> {
			Map<String, Object> parameters = new HashMap<>(2);
			parameters.put("chargePointId", cpc.getId().getChargePointId());
			parameters.put("connectorId", cpc.getId().getConnectorId());
			return StringUtils.expandTemplateString(controlIdTemplate, parameters);
		}).collect(Collectors.toList());
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		if ( controlId == null || controlId.isEmpty() ) {
			return null;
		}
		Matcher m = controlIdRegex.matcher(controlId);
		if ( !m.matches() ) {
			return null;
		}
		final String chargePointId = m.group(1);
		final int connectorId = Integer.parseInt(m.group(2));

		ChargePointConnector cpc = chargePointConnectorDao
				.get(new ChargePointConnectorKey(chargePointId, connectorId));
		return newNodeControlInfoDatum(controlId, chargePointId, connectorId,
				cpc.getInfo() != null ? cpc.getInfo().getStatus() != ChargePointStatus.Unavailable
						: false);
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		if ( !InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(instruction.getTopic()) ) {
			return null;
		}
		boolean handled = false;
		boolean allComplete = true;
		for ( String paramName : instruction.getParameterNames() ) {
			log.trace("Got instruction parameter {}", paramName);
			Matcher m = controlIdRegex.matcher(paramName);
			if ( !m.matches() ) {
				continue;
			}

			final String chargePointId = m.group(1);
			final int connectorId = Integer.parseInt(m.group(2));
			final boolean enabled = StringUtils.parseBoolean(instruction.getParameterValue(paramName));

			for ( ChargePointManager mgr : chargePointManagers.services() ) {
				if ( mgr.isChargePointAvailable(chargePointId) ) {
					handled = true;

					CompletableFuture<Boolean> f = mgr.adjustConnectorEnabledState(chargePointId,
							connectorId, enabled);
					try {
						boolean result = f.get(messageTimeout, TimeUnit.MILLISECONDS).booleanValue();
						allComplete &= result;
						if ( result ) {
							postControlEvent(
									newNodeControlInfoDatum(paramName, chargePointId, connectorId,
											enabled),
									NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
						}
					} catch ( Exception e ) {
						log.warn("Unable to adjust Charge Point {} connector {} enabled state to {}: {}",
								chargePointId, connectorId, enabled, e.toString());
					}
					break;
				}
			}
		}
		if ( handled ) {
			if ( allComplete ) {
				return InstructionState.Completed;
			} else {
				return InstructionState.Declined;
			}
		}
		return null;
	}

	private NodeControlInfoDatum newNodeControlInfoDatum(String controlId, String chargePointId,
			int connectorId, boolean enabled) {
		NodeControlInfoDatum info = new NodeControlInfoDatum();
		info.setCreated(new Date());
		info.setSourceId(controlId);
		info.setType(NodeControlPropertyType.Boolean);
		info.setReadonly(false);
		info.setValue(String.valueOf(enabled));
		return info;
	}

	private void postControlEvent(NodeControlInfoDatum info, String topic) {
		final EventAdmin admin = (eventAdmin != null ? eventAdmin.service() : null);
		if ( admin == null ) {
			return;
		}
		Map<String, ?> props = info.asSimpleMap();
		admin.postEvent(new Event(topic, props));
	}

	// SettingsSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.control.connectors.availability";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(2);
		results.add(
				new BasicTextFieldSettingSpecifier("controlIdTemplate", DEFAULT_CONTROL_ID_TEMPLATE));
		results.add(new BasicTextFieldSettingSpecifier("controlIdRegexValue",
				DEFAULT_CONTROL_ID_REGEX.pattern()));
		return results;
	}

	// EventHandler

	@Override
	public void handleEvent(Event arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * Get the control ID template.
	 * 
	 * @return the template; defaults to {@link #DEFAULT_CONTROL_ID_TEMPLATE}
	 */
	public String getControlIdTemplate() {
		return controlIdTemplate;
	}

	/**
	 * Set the control ID template.
	 * 
	 * <p>
	 * This template string allows for 2 parameters:
	 * </p>
	 * 
	 * <ol>
	 * <li><code>{chargePointId}</code> - the Charge Point ID (string)</li>
	 * <li><code>{connectorId}</code> - the connector ID (integer)</li>
	 * </ol>
	 * 
	 * @param controlIdTemplate
	 *        the controlIdTemplate to set
	 */
	public void setControlIdTemplate(String controlIdTemplate) {
		if ( controlIdTemplate == null ) {
			controlIdTemplate = DEFAULT_CONTROL_ID_TEMPLATE;
		}
		this.controlIdTemplate = controlIdTemplate;
	}

	/**
	 * Get the control ID regular expression.
	 * 
	 * @return the expression; defaults to {@link #DEFAULT_CONTROL_ID_REGEX}
	 */
	public Pattern getControlIdRegex() {
		return controlIdRegex;
	}

	/**
	 * Set the control ID regular expression.
	 * 
	 * <p>
	 * This expression is used to extract the same template parameters used in
	 * {@link #setControlIdTemplate(String)} from an encoded control ID value.
	 * The pattern must provide matching groups for the same elements, in the
	 * same order, as that template.
	 * </p>
	 * 
	 * @param controlIdRegex
	 *        the expression to set
	 */
	public void setControlIdRegex(Pattern controlIdRegex) {
		if ( controlIdRegex == null ) {
			controlIdRegex = DEFAULT_CONTROL_ID_REGEX;
		}
		this.controlIdRegex = controlIdRegex;
	}

	/**
	 * Get the control ID regular expression, as a string.
	 * 
	 * @return the expression
	 * @see #getControlIdRegex()
	 */
	public String getControlIdRegexValue() {
		return getControlIdRegex().pattern();
	}

	/**
	 * Set the control ID regular expression, as a string.
	 * 
	 * @param controlIdRegex
	 *        the expression to set
	 * @see #setControlIdRegex(Pattern)
	 */
	public void setControlIdRegexValue(String value) {
		try {
			setControlIdRegex(Pattern.compile(value, Pattern.CASE_INSENSITIVE));
		} catch ( IllegalArgumentException e ) {
			setControlIdRegex(DEFAULT_CONTROL_ID_REGEX);
		}
	}

	/**
	 * Get the event admin.
	 * 
	 * @return the event admin
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set the event admin.
	 * 
	 * @param eventAdmin
	 *        the event admin to use
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Get the message timeout.
	 * 
	 * @return the timeout, in milliseconds; defaults to
	 *         {@link #DEFAULT_MESSAGE_TIMEOUT}
	 */
	public long getMessageTimeout() {
		return messageTimeout;
	}

	/**
	 * Set the message timeout.
	 * 
	 * @param messageTimeout
	 *        the timeout to set, in milliseconds
	 */
	public void setMessageTimeout(long messageTimeout) {
		this.messageTimeout = messageTimeout;
	}

}
