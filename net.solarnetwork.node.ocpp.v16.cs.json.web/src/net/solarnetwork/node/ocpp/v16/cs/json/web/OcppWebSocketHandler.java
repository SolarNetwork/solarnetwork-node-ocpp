/* ==================================================================
 * OcppWebSocketHandler.java - 31/01/2020 3:34:19 pm
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

package net.solarnetwork.node.ocpp.v16.cs.json.web;

import static java.util.Collections.singletonList;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import net.solarnetwork.node.ocpp.json.OcppWebSocketSubProtocol;
import ocpp.domain.ErrorCode;
import ocpp.json.MessageType;
import ocpp.v16.ActionErrorCode;

/**
 * OCPP 1.6 JSON web socket handler.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppWebSocketHandler extends AbstractWebSocketHandler
		implements WebSocketHandler, SubProtocolCapable {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ObjectMapper mapper;

	private static ObjectMapper defaultObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JaxbAnnotationModule());
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		return mapper;
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * A default {@link ObjectMapper} will be created.
	 * </p>
	 */
	public OcppWebSocketHandler() {
		this(defaultObjectMapper());
	}

	/**
	 * Constructor.
	 * 
	 * @param mapper
	 *        the object mapper to use
	 */
	public OcppWebSocketHandler(ObjectMapper mapper) {
		super();
		this.mapper = mapper;
	}

	@Override
	public List<String> getSubProtocols() {
		return singletonList(OcppWebSocketSubProtocol.OCPP_V16.getValue());
	}

	private String clientId(WebSocketSession session) {
		Object id = session.getAttributes().get(OcppWebSocketHandshakeInterceptor.CLIENT_ID_ATTR);
		return (id != null ? id.toString() : null);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		final String clientId = clientId(session);
		log.trace("OCPP {} <<< {}", clientId, message.getPayload());
		JsonNode tree = mapper.readTree(message.getPayload());
		if ( tree.isArray() ) {
			JsonNode msgTypeNode = tree.path(0);
			JsonNode messageIdNode = tree.path(1);
			final String messageId = messageIdNode.isTextual() ? messageIdNode.textValue() : "NULL";
			if ( !msgTypeNode.isInt() ) {
				sendCallError(session, clientId, messageId, ActionErrorCode.FormationViolation,
						"Message type not provided.", null);
				return;
			}
			ocpp.json.MessageType msgType;
			try {
				msgType = MessageType.forNumber(msgTypeNode.intValue());
			} catch ( IllegalArgumentException e ) {
				// OCPP spec says messages with unknown types should be ignored
				log.info("OCPP {} <<< Ignoring message with unknown type: {}", clientId,
						message.getPayload());
				return;
			}
			switch (msgType) {
				case Call:
					handleCallMessage(session, clientId, message, tree);
					break;

				case CallError:
					handleCallErrorMessage(session, clientId, message, tree);
					break;

				case CallResult:
					handleCallResultMessage(session, clientId, message, tree);
					break;
			}
		}
	}

	private void handleCallMessage(WebSocketSession session, String clientId, TextMessage message,
			JsonNode tree) {
		// TODO Auto-generated method stub

	}

	private void handleCallErrorMessage(WebSocketSession session, String clientId, TextMessage message,
			JsonNode tree) {
		// TODO Auto-generated method stub

	}

	private void handleCallResultMessage(WebSocketSession session, String clientId, TextMessage message,
			JsonNode tree) {
		// TODO Auto-generated method stub

	}

	private void sendCallError(WebSocketSession session, String clientId, String messageId,
			ErrorCode errorCode, String errorDescription, Map<String, Object> details) {
		Object[] msg = new Object[] { MessageType.CallError.getNumber(), messageId, errorCode.getName(),
				errorDescription, details != null ? details : Collections.emptyMap() };
		try {
			String json = mapper.writeValueAsString(msg);
			log.trace("OCPP {} >>> {}", clientId, json);
			session.sendMessage(new TextMessage(json));
		} catch ( IOException e ) {
			log.warn("OCPP {} >>> Communication error sending CALLERROR for message ID {}: {}", clientId,
					messageId, e.getMessage());
		}
	}

}
