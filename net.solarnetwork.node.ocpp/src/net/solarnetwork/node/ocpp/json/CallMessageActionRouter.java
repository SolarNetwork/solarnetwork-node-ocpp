/* ==================================================================
 * CallMessageActionRouter.java - 4/02/2020 4:30:10 pm
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

package net.solarnetwork.node.ocpp.json;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.solarnetwork.node.ocpp.domain.ActionMessage;
import net.solarnetwork.node.ocpp.domain.BasicActionMessage;
import net.solarnetwork.node.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.node.ocpp.service.ActionMessageResultHandler;
import ocpp.domain.Action;
import ocpp.domain.ErrorCode;
import ocpp.domain.ErrorHolder;
import ocpp.json.BasicCallErrorMessage;
import ocpp.json.BasicCallResultMessage;
import ocpp.json.CallErrorMessage;
import ocpp.json.CallMessage;
import ocpp.json.CallResultMessage;

/**
 * Service to route {@link CallMessage} objects to
 * {@link ActionMessageProcessor} services, translating between JSON and generic
 * message formats.
 * 
 * @author matt
 * @version 1.0
 */
public class CallMessageActionRouter implements CallMessageProcessor {

	private final ConcurrentMap<Action, CopyOnWriteArrayList<ActionMessageProcessor>> processors;
	private final ErrorCode internalErrorCode;
	private final ErrorCode notImplementedErrorCode;

	/**
	 * Constructor.
	 * 
	 * @param internalErrorCode
	 *        the internal error code to use
	 * @param notImplementedErrorCode
	 *        the "not implemented" error code to use
	 * @throws IllegalArgumentException
	 *         if {@code internalErrorCode} is {@literal null}
	 */
	public CallMessageActionRouter(ErrorCode internalErrorCode, ErrorCode notImplementedErrorCode) {
		super();
		processors = new ConcurrentHashMap<>(16, 0.9f, 1);
		this.internalErrorCode = internalErrorCode;
		this.notImplementedErrorCode = notImplementedErrorCode;
	}

	@Override
	public void processCallMessage(CallMessage message, CallMessageResultHandler resultHandler) {
		Action action = message.getAction();
		List<ActionMessageProcessor> procs = processors.get(action);
		if ( procs == null ) {
			BasicCallErrorMessage err = new BasicCallErrorMessage(message.getMessageId(),
					notImplementedErrorCode, "Action not supported.", null);
			resultHandler.handleCallMessageResult(message, null, err);
			return;
		}
		ActionMessage<Object, Object> msg = createActionMessage(message);
		for ( ActionMessageProcessor p : procs ) {
			try {
				p.processActionMessage(msg,
						(ActionMessageResultHandler<Object, Object>) (am, result, error) -> {
							CallResultMessage resultMsg = createCallResultMessage(message, result);
							CallErrorMessage errorMsg = createCallErrorMessage(message, error);
							return resultHandler.handleCallMessageResult(message, resultMsg, errorMsg);
						});
			} catch ( Throwable t ) {
				BasicCallErrorMessage err = new BasicCallErrorMessage(message.getMessageId(),
						internalErrorCode, "Error handling action.", null);
				resultHandler.handleCallMessageResult(message, null, err);
			}
		}
	}

	private CallResultMessage createCallResultMessage(CallMessage message, Object result) {
		return new BasicCallResultMessage(message.getMessageId(), result);
	}

	private CallErrorMessage createCallErrorMessage(CallMessage message, Throwable error) {
		ErrorCode errorCode = null;
		String errorDescription = null;
		Map<String, ?> errorDetails = null;
		if ( error instanceof ErrorHolder ) {
			errorCode = ((ErrorHolder) error).getErrorCode();
			errorDescription = ((ErrorHolder) error).getErrorDescription();
			errorDetails = ((ErrorHolder) error).getErrorDetails();
		}
		if ( errorCode == null ) {
			errorCode = internalErrorCode;
		}
		return new BasicCallErrorMessage(message.getMessageId(), errorCode, errorDescription,
				errorDetails);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ActionMessage<Object, Object> createActionMessage(CallMessage message) {
		return new BasicActionMessage<Object, Object>(message.getAction(), message.getPayload(),
				(Class) actionResultClass(message.getAction()));
	}

	private Class<?> actionResultClass(Action action) {
		// TODO
		return null;
	}

	/**
	 * Add an action message processor.
	 * 
	 * <p>
	 * Once added, messages for its supported actions will be routed to it.
	 * </p>
	 * 
	 * @param processor
	 *        to processor to add; {@literal null} will be ignored
	 */
	public void addActionMessageProcessor(ActionMessageProcessor processor) {
		if ( processor == null ) {
			return;
		}
		for ( Action action : processor.getSupportedActions() ) {
			processors.compute(action, (k, v) -> {
				CopyOnWriteArrayList<ActionMessageProcessor> procs = v;
				if ( procs == null ) {
					procs = new CopyOnWriteArrayList<>();
				}
				procs.addIfAbsent(processor);
				return procs;
			});
		}
	}

	/**
	 * Remove an action message processor.
	 * 
	 * <p>
	 * Once removed, messages will no longer be routed to it.
	 * </p>
	 * 
	 * @param processor
	 *        the processor to remove; {@literal null} will be ignored
	 */
	public void removeActionMessageProcessor(ActionMessageProcessor processor) {
		if ( processor == null ) {
			return;
		}
		for ( List<ActionMessageProcessor> procs : processors.values() ) {
			procs.remove(processor);
		}
	}

}
