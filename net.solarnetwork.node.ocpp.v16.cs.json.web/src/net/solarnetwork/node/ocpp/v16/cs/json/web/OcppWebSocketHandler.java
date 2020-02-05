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
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import net.solarnetwork.node.ocpp.json.CallMessageProcessor;
import net.solarnetwork.node.ocpp.json.CallMessageResultHandler;
import net.solarnetwork.node.ocpp.json.OcppWebSocketSubProtocol;
import ocpp.domain.ErrorCode;
import ocpp.json.ActionPayloadDecoder;
import ocpp.json.BasicCallErrorMessage;
import ocpp.json.BasicCallMessage;
import ocpp.json.BasicCallResultMessage;
import ocpp.json.CallErrorMessage;
import ocpp.json.CallMessage;
import ocpp.json.CallResultMessage;
import ocpp.json.MessageType;
import ocpp.v16.ActionErrorCode;
import ocpp.v16.CentralSystemAction;
import ocpp.v16.cp.json.ChargePointActionPayloadDecoder;
import ocpp.v16.cs.json.CentralServiceActionPayloadDecoder;

/**
 * OCPP 1.6 Central Service JSON web socket handler.
 * 
 * <p>
 * This class is responsible for encoding/decoding the OCPP 1.6 JSON web socket
 * message protocol. When a Charge Point sends a message to this service, the
 * JSON will be decoded using {@link #getCentralServiceActionPayloadDecoder()},
 * and the resulting {@link CallMessage} will be passed to the configured
 * {@link #getCallMessageProcessor()} instance via
 * {@link CallMessageProcessor#processCallMessage(CallMessage, CallMessageResultHandler)}
 * where this instance will be passed as the {@link CallMessageResultHandler}.
 * The call processor must eventually call
 * {@link #handleCallMessageResult(CallMessage, CallResultMessage, CallErrorMessage)}
 * with the final result (or error), and that will be encoded into a JSON
 * message and sent back to the originating Charge Point client.
 * </p>
 * 
 * <p>
 * This class also implements {@link CallMessageProcessor} itself, so that other
 * classes can push messages to any connected Charge Point client. The
 * {@link #processCallMessage(CallMessage, CallMessageResultHandler)} will
 * encode a {@link CallMessage} into JSON and sent that as a request to a
 * connected Charge Point matching the message's client ID. When the Charge
 * Point client sends a result (or error) response to the message, it will be
 * passed to the {@link CallMessageResultHandler} originally provided.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class OcppWebSocketHandler extends AbstractWebSocketHandler
		implements WebSocketHandler, SubProtocolCapable, CallMessageResultHandler, CallMessageProcessor {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ConcurrentMap<String, WebSocketSession> clientSessions;
	private final Deque<PendingCallMessage> pendingMessages;
	private final ObjectMapper mapper;
	private ActionPayloadDecoder centralServiceActionPayloadDecoder;
	private ActionPayloadDecoder chargePointActionPayloadDecoder;
	private CallMessageProcessor callMessageProcessor;

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
	 * Default {@link ObjectMapper} and
	 * {@link CentralServiceActionPayloadDecoder} and
	 * {@link ChargePointActionPayloadDecoder} instances will be created. An
	 * in-memory queue will be used for pending messages.
	 * </p>
	 */
	public OcppWebSocketHandler() {
		super();
		this.clientSessions = new ConcurrentHashMap<>(8, 0.7f, 2);
		this.pendingMessages = new LinkedList<>();
		this.mapper = defaultObjectMapper();
		this.centralServiceActionPayloadDecoder = new CentralServiceActionPayloadDecoder(mapper);
		this.chargePointActionPayloadDecoder = new ChargePointActionPayloadDecoder(mapper);
	}

	/**
	 * Constructor.
	 * 
	 * @param mapper
	 *        the object mapper to use
	 * @param pendingMessageQueue
	 *        a queue to hold pending messages
	 * @param centralServiceActionPayloadDecoder
	 *        the action payload decoder to use
	 * @param chargePointActionPayloadDecoder
	 *        for Central Service message the action payload decoder to use for
	 *        Charge Point messages
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public OcppWebSocketHandler(ObjectMapper mapper, Deque<PendingCallMessage> pendingMessageQueue,
			ActionPayloadDecoder actionPayloadDecoder,
			ActionPayloadDecoder chargePointActionPayloadDecoder) {
		super();
		this.clientSessions = new ConcurrentHashMap<>(8, 0.7f, 2);
		if ( mapper == null ) {
			throw new IllegalArgumentException("The mapper parameter must not be null.");
		}
		this.mapper = mapper;
		if ( pendingMessageQueue == null ) {
			throw new IllegalArgumentException("The pendingMessageQueue parameter must not be null.");
		}
		this.pendingMessages = pendingMessageQueue;
		setCentralServiceActionPayloadDecoder(actionPayloadDecoder);
		setChargePointActionPayloadDecoder(chargePointActionPayloadDecoder);
	}

	@Override
	public List<String> getSubProtocols() {
		return singletonList(OcppWebSocketSubProtocol.OCPP_V16.getValue());
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		// save client session association
		String clientId = clientId(session);
		if ( clientId != null ) {
			clientSessions.put(clientId, session);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		// remove client session association
		String clientId = clientId(session);
		if ( clientId != null ) {
			clientSessions.remove(clientId, session);
		}
	}

	private String clientId(WebSocketSession session) {
		Object id = session.getAttributes().get(OcppWebSocketHandshakeInterceptor.CLIENT_ID_ATTR);
		return (id != null ? id.toString() : null);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		final String clientId = clientId(session);
		log.trace("OCPP {} <<< {}", clientId, message.getPayload());
		JsonNode tree;
		try {
			tree = mapper.readTree(message.getPayload());
		} catch ( JsonProcessingException e ) {
			sendCallError(session, clientId, null, ActionErrorCode.ProtocolError,
					"Message malformed JSON.", null);
			return;
		}
		if ( tree.isArray() ) {
			JsonNode msgTypeNode = tree.path(0);
			JsonNode messageIdNode = tree.path(1);
			final String messageId = messageIdNode.isTextual() ? messageIdNode.textValue() : "NULL";
			if ( !msgTypeNode.isInt() ) {
				sendCallError(session, clientId, messageId, ActionErrorCode.FormationViolation,
						"Message type not provided.", null);
				return;
			}
			MessageType msgType;
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
					handleCallMessage(session, clientId, messageId, message, tree);
					break;

				case CallError:
					handleCallErrorMessage(session, clientId, messageId, message, tree);
					break;

				case CallResult:
					handleCallResultMessage(session, clientId, messageId, message, tree);
					break;
			}
		}
	}

	/**
	 * Process a request from a client Charge Point.
	 * 
	 * <p>
	 * The message payload will be decoded using
	 * {@link #getCentralServiceActionPayloadDecoder()} and then passed to the
	 * {@link #getCallMessageProcessor()}, passing this object as the
	 * {@link CallMessageResultHandler} so the processor's result (or error) can
	 * be returned to the client.
	 * </p>
	 * 
	 * @param session
	 *        the session
	 * @param clientId
	 *        the Charge Point client ID
	 * @param messageId
	 *        the message ID
	 * @param message
	 *        the web socket message
	 * @param tree
	 *        the parsed JSON from the message
	 * @return {@literal true} if the message was processed
	 * @see #handleCallMessageResult(CallMessage, CallResultMessage,
	 *      CallErrorMessage)
	 */
	private boolean handleCallMessage(final WebSocketSession session, final String clientId,
			final String messageId, final TextMessage message, final JsonNode tree) {
		final JsonNode actionNode = tree.path(2);
		final CallMessageProcessor proc = getCallMessageProcessor();
		if ( proc == null ) {
			log.debug("OCPP {} <<< No CallMessageProcessor available, ignoring message: {}", clientId,
					message.getPayload());
			return sendCallError(session, clientId, messageId, ActionErrorCode.InternalError,
					"No CallMessageProcessor available.", null);
		}
		final CentralSystemAction action;
		try {
			action = actionNode.isTextual() ? CentralSystemAction.valueOf(actionNode.textValue())
					: null;
			if ( action == null ) {
				return sendCallError(session, clientId, messageId, ActionErrorCode.FormationViolation,
						actionNode.isMissingNode() ? "Missing action." : "Malformed action.", null);
			}
			Object payload;
			try {
				payload = centralServiceActionPayloadDecoder.decodeActionPayload(action, false,
						tree.path(3));
			} catch ( IOException e ) {
				return sendCallError(session, clientId, messageId, ActionErrorCode.FormationViolation,
						"Error parsing payload: " + e.getMessage(), null);
			}
			BasicCallMessage msg = new BasicCallMessage(clientId, messageId, action, payload);
			proc.processCallMessage(msg, this);
			return true;
		} catch ( IllegalArgumentException e ) {
			return sendCallError(session, clientId, messageId, ActionErrorCode.NotImplemented,
					"Unknown action.", null);
		}
	}

	/**
	 * Send the result of processing a client Charge Point request back to the
	 * client.
	 * 
	 * <p>
	 * If an {@code error} is provided, this method will send a
	 * {@link MessageType#CallError} message to the client that originally made
	 * the request. If instead a {@code result} is provided, this method will
	 * send a {@link MessageType#CallResult} message to the client that
	 * originally made the request.
	 * </p>
	 * 
	 * @param message
	 *        the original request message
	 * @param result
	 *        the result to send to the client, or {@literal null} if an error
	 *        occurred
	 * @param error
	 *        the error to send to the client, or {@literal null} if no error
	 *        occurred
	 * @return {@literal true} if the result was sent to the client
	 */
	@Override
	public boolean handleCallMessageResult(CallMessage message, CallResultMessage result,
			CallErrorMessage error) {
		final String clientId = message.getClientId();
		if ( clientId == null ) {
			log.error("Client ID not provided in result for CallMessage {}; ignoring: {}", message,
					result != null ? result : error);
			return false;
		}
		WebSocketSession session = clientSessions.get(message.getClientId());
		if ( session == null ) {
			log.error("Web socket not available for result of CallMessage {}; ignoring: {}", message,
					result != null ? result : error);
			return false;
		}
		if ( error != null ) {
			return sendCallError(session, clientId, message.getMessageId(), error.getErrorCode(),
					error.getErrorDescription(), error.getErrorDetails());
		} else if ( result != null ) {
			return sendCallResult(session, clientId, result.getMessageId(), result.getPayload());
		}
		log.error("Neither result nor error provided for result of CallMessage {}; ignoring.", message);
		return false;
	}

	/**
	 * Push a message to a Charge Point.
	 * 
	 * <p>
	 * This method is for sending a push message from the Central Service to a
	 * Charge Point. The Charge Point must be currently connected to the Central
	 * Service via a web socket.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void processCallMessage(CallMessage message, CallMessageResultHandler resultHandler) {
		final String clientId = message.getClientId();
		if ( clientId == null ) {
			log.debug("Client ID not provided in CallMessage {}; ignoring", message);
			BasicCallErrorMessage err = new BasicCallErrorMessage(message.getMessageId(),
					ActionErrorCode.SecurityError, "Client ID missing.", null);
			resultHandler.handleCallMessageResult(message, null, err);
			return;
		}
		final PendingCallMessage msg = new PendingCallMessage(System.currentTimeMillis(), message,
				resultHandler);

		synchronized ( pendingMessages ) {
			// enqueue the call
			pendingMessages.add(msg);
			if ( pendingMessages.peek() == msg ) {
				// there are no other pending messages, so immediately send this call
				sendCall(msg);
			}
		}
	}

	/**
	 * Push a pending message to a Charge Point.
	 * 
	 * @param msg
	 *        the pending message to send; this message is expected to have been
	 *        added to the pending message queue already
	 */
	private void sendCall(PendingCallMessage msg) {
		final CallMessage message = msg.getMessage();
		final CallMessageResultHandler resultHandler = msg.getHandler();
		WebSocketSession session = clientSessions.get(message.getClientId());
		if ( session != null ) {
			log.debug("Web socket not available for CallMessage {}; ignoring", message);
			return;
		}
		boolean sent = sendCall(session, message.getClientId(), message.getMessageId(),
				message.getAction(), message.getPayload());
		if ( !sent ) {
			synchronized ( pendingMessages ) {
				pendingMessages.removeFirstOccurrence(msg);
			}
			BasicCallErrorMessage err = new BasicCallErrorMessage(message.getMessageId(),
					ActionErrorCode.SecurityError, "Client ID missing.", null);
			try {
				resultHandler.handleCallMessageResult(message, null, err);
			} catch ( Exception e ) {
				log.warn("Error handling OCPP CallError {}: {}", err, e.toString(), e);
			}
			sendNextPendingMessage();
		}
	}

	/**
	 * Send the next pending message, if available.
	 * 
	 * <p>
	 * This method assumes that the head of the pending message queue has not
	 * been sent to the client yet. This this method should only be called after
	 * processing the result of a sent message, after the sent message has been
	 * removed from the queue.
	 * </p>
	 * 
	 * @see #sendCall(PendingCallMessage)
	 */
	private void sendNextPendingMessage() {
		synchronized ( pendingMessages ) {
			PendingCallMessage msg = pendingMessages.peek();
			if ( msg != null ) {
				sendCall(msg);
			}
		}
	}

	/**
	 * Find and remove a pending message based on its message ID.
	 * 
	 * <p>
	 * This method will search the pending messages queue and return the first
	 * message found with the matching message ID, after first removing that
	 * message from the queue.
	 * </p>
	 * 
	 * @param messageId
	 *        the ID to find
	 * @return the found message, or {@literal null} if not found
	 */
	private PendingCallMessage pollPendingMessage(final String messageId) {
		PendingCallMessage msg = null;
		synchronized ( pendingMessages ) {
			for ( Iterator<PendingCallMessage> itr = pendingMessages.descendingIterator(); itr
					.hasNext(); ) {
				PendingCallMessage oneMsg = itr.next();
				if ( oneMsg.getMessage().getMessageId().equals(messageId) ) {
					msg = oneMsg;
					itr.remove();
					break;
				}
			}
		}
		return msg;
	}

	/**
	 * Process a CallError response to a Call message previously sent to a
	 * client.
	 * 
	 * <p>
	 * If there is another message available in the pending message queue, that
	 * message will be sent to the client.
	 * </p>
	 * 
	 * @param session
	 *        the session
	 * @param clientId
	 *        the Charge Point client ID
	 * @param messageId
	 *        the message ID
	 * @param message
	 *        the message
	 * @param tree
	 *        the JSON
	 */
	@SuppressWarnings("unchecked")
	private void handleCallErrorMessage(final WebSocketSession session, final String clientId,
			final String messageId, final TextMessage message, final JsonNode tree) {
		try {
			PendingCallMessage msg = pollPendingMessage(messageId);
			if ( msg == null ) {
				log.warn(
						"OCPP {} <<< Original Call message {} not found; ignoring CallError message: {}",
						clientId, messageId, message.getPayload());
				return;
			}
			ActionErrorCode errorCode;
			try {
				errorCode = ActionErrorCode.valueOf(tree.path(2).asText());
			} catch ( IllegalArgumentException e ) {
				log.warn("OCPP {} <<< Error code {} not valid; ignoring CallError message: {}", clientId,
						tree.path(2).asText(), message.getPayload());
				return;
			}
			Map<String, ?> details = null;
			try {
				details = mapper.treeToValue(tree.path(4), Map.class);
			} catch ( JsonProcessingException e ) {
				log.warn("OCPP {} <<< Error parsing CallError details object {}, ignoring: {}", clientId,
						tree.path(4), e.toString());
			}
			BasicCallErrorMessage err = new BasicCallErrorMessage(messageId, errorCode,
					tree.path(3).asText(), details);
			msg.getHandler().handleCallMessageResult(msg.getMessage(), null, err);
		} finally {
			sendNextPendingMessage();
		}
	}

	/**
	 * Process a CallResult response to a Call message previously sent to a
	 * client.
	 * 
	 * 
	 * <p>
	 * If there is another message available in the pending message queue, that
	 * message will be sent to the client.
	 * </p>
	 * 
	 * @param session
	 *        the session
	 * @param clientId
	 *        the Charge Point client ID
	 * @param messageId
	 *        the message ID
	 * @param message
	 *        the message
	 * @param tree
	 *        the JSON
	 */
	private void handleCallResultMessage(final WebSocketSession session, final String clientId,
			final String messageId, final TextMessage message, final JsonNode tree) {
		try {
			PendingCallMessage msg = pollPendingMessage(messageId);
			if ( msg == null ) {
				log.warn(
						"OCPP {} <<< Original Call message {} not found; ignoring CallError message: {}",
						clientId, messageId, message.getPayload());
				return;
			}

			CallResultMessage result = null;
			CallErrorMessage err = null;

			Object payload;
			try {
				payload = chargePointActionPayloadDecoder
						.decodeActionPayload(msg.getMessage().getAction(), true, tree.path(2));
				result = new BasicCallResultMessage(messageId, payload);
			} catch ( IOException e ) {
				err = new BasicCallErrorMessage(messageId, ActionErrorCode.FormationViolation,
						"Error parsing payload: " + e.getMessage(), null);
			}

			msg.getHandler().handleCallMessageResult(msg.getMessage(), result, err);
		} finally {
			sendNextPendingMessage();
		}
	}

	private boolean sendCall(final WebSocketSession session, final String clientId,
			final String messageId, final ocpp.domain.Action action, final Object payload) {
		Object[] msg = new Object[] { MessageType.Call.getNumber(), messageId, action.getName(),
				payload };
		try {
			String json = mapper.writeValueAsString(msg);
			log.trace("OCPP {} >>> {}", clientId, json);
			session.sendMessage(new TextMessage(json));
			return true;
		} catch ( IOException e ) {
			log.warn("OCPP {} >>> Communication error sending Call for message ID {}: {}", clientId,
					messageId, e.getMessage());
		}
		return false;
	}

	private boolean sendCallResult(final WebSocketSession session, final String clientId,
			final String messageId, final Object payload) {
		Object[] msg = new Object[] { MessageType.CallResult.getNumber(), messageId, payload };
		try {
			String json = mapper.writeValueAsString(msg);
			log.trace("OCPP {} >>> {}", clientId, json);
			session.sendMessage(new TextMessage(json));
			return true;
		} catch ( IOException e ) {
			log.warn("OCPP {} >>> Communication error sending CallResult for message ID {}: {}",
					clientId, messageId, e.getMessage());
		}
		return false;
	}

	private boolean sendCallError(final WebSocketSession session, final String clientId,
			final String messageId, final ErrorCode errorCode, final String errorDescription,
			final Map<String, ?> details) {
		Object[] msg = new Object[] { MessageType.CallError.getNumber(), messageId, errorCode.getName(),
				errorDescription, details != null ? details : Collections.emptyMap() };
		try {
			String json = mapper.writeValueAsString(msg);
			log.trace("OCPP {} >>> {}", clientId, json);
			session.sendMessage(new TextMessage(json));
			return true;
		} catch ( IOException e ) {
			log.warn("OCPP {} >>> Communication error sending CallError for message ID {}: {}", clientId,
					messageId, e.getMessage());
		}
		return false;
	}

	/**
	 * Get the configured call message processor.
	 * 
	 * @return the processor
	 */
	public CallMessageProcessor getCallMessageProcessor() {
		return callMessageProcessor;
	}

	/**
	 * Set the call message processor.
	 * 
	 * @param callMessageProcessor
	 *        the processor to use
	 */
	public void setCallMessageProcessor(CallMessageProcessor callMessageProcessor) {
		this.callMessageProcessor = callMessageProcessor;
	}

	/**
	 * Get the configured action payload decoder for Central Service messages.
	 * 
	 * @return the decoder, never {@literal null}
	 */
	public ActionPayloadDecoder getCentralServiceActionPayloadDecoder() {
		return centralServiceActionPayloadDecoder;
	}

	/**
	 * Set the action payload decoder for Central Service messages.
	 * 
	 * @param centralServiceActionPayloadDecoder
	 *        the decoder to use
	 * @throws IllegalArgumentException
	 *         if {@code centralServiceActionPayloadDecoder} is {@literal null}
	 */
	public void setCentralServiceActionPayloadDecoder(
			ActionPayloadDecoder centralServiceActionPayloadDecoder) {
		if ( centralServiceActionPayloadDecoder == null ) {
			throw new IllegalArgumentException(
					"The centralServiceActionPayloadDecoder parameter must not be null.");
		}
		this.centralServiceActionPayloadDecoder = centralServiceActionPayloadDecoder;
	}

	/**
	 * Get the configured action payload decoder for Charge Point messages.
	 * 
	 * @return the decoder, never {@literal null}
	 */
	public ActionPayloadDecoder getChargePointActionPayloadDecoder() {
		return chargePointActionPayloadDecoder;
	}

	/**
	 * Set the action payload decoder for Charge Point messages.
	 * 
	 * @param centralServiceActionPayloadDecoder
	 *        the decoder to use
	 * @throws IllegalArgumentException
	 *         if {@code centralServiceActionPayloadDecoder} is {@literal null}
	 */
	public void setChargePointActionPayloadDecoder(
			ActionPayloadDecoder chargePointActionPayloadDecoder) {
		if ( chargePointActionPayloadDecoder == null ) {
			throw new IllegalArgumentException(
					"The chargePointActionPayloadDecoder parameter must not be null.");
		}
		this.chargePointActionPayloadDecoder = chargePointActionPayloadDecoder;
	}

}
