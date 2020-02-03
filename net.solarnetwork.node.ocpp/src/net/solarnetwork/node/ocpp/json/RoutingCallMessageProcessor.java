/* ==================================================================
 * RoutingCallMessageProcessor.java - 2/02/2020 5:36:31 pm
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

import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ocpp.domain.Action;
import ocpp.domain.ErrorCode;
import ocpp.json.BasicCallErrorMessage;
import ocpp.json.CallMessage;

/**
 * A {@link CallMessageProcessor} that routes messages to other delegate
 * {@link CallMessageProcessor} instances based on the {@link Action} of each
 * {@link CallMessage}.
 * 
 * @author matt
 * @version 1.0
 */
public class RoutingCallMessageProcessor implements CallMessageProcessor {

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final ErrorCode notImplementedErrorCode;
	private final ErrorCode internalErrorErrorCode;
	private Map<String, CallMessageProcessor> processors;

	/**
	 * Constructor.
	 * 
	 * @param notImplementedErrorCode
	 *        the error code to use when a delegate processor is not configured
	 *        for a given action; this must be configured to match the OCPP
	 *        protocol being used
	 * @param internalErrorErrorCode
	 *        the error code to use when a delegate processor throws an
	 *        exception; this must be configured to match the OCPP protocol
	 *        being used
	 */
	public RoutingCallMessageProcessor(ErrorCode notImplementedErrorCode,
			ErrorCode internalErrorErrorCode) {
		this(notImplementedErrorCode, notImplementedErrorCode, Collections.emptyMap());
	}

	/**
	 * Constructor.
	 * 
	 * @param notImplementedErrorCode
	 *        the error code to use when a delegate processor is not configured
	 *        for a given action; this must be configured to match the OCPP
	 *        protocol being used
	 * @param internalErrorErrorCode
	 *        the error code to use when a delegate processor throws an
	 *        exception; this must be configured to match the OCPP protocol
	 *        being used
	 * @param processors
	 *        the mapping of actions to associated processors
	 */
	public RoutingCallMessageProcessor(ErrorCode notImplementedErrorCode,
			ErrorCode internalErrorErrorCode, Map<String, CallMessageProcessor> processors) {
		super();
		if ( notImplementedErrorCode == null ) {
			throw new IllegalArgumentException(
					"The notImplementedErrorCode parameter must not be null.");
		}
		this.notImplementedErrorCode = notImplementedErrorCode;
		if ( internalErrorErrorCode == null ) {
			throw new IllegalArgumentException("The internalErrorErrorCode parameter must not be null.");
		}
		this.internalErrorErrorCode = internalErrorErrorCode;
		setProcessors(processors);
	}

	/**
	 * Process a {@link CallMessage} and provide the result to a
	 * {@link CallMessageResultHandler}.
	 * 
	 * <p>
	 * This implementation looks for a processor in {@link #getProcessors()}
	 * based on the {@link CallMessage#getAction()} name of the given message.
	 * The message is then passed to that delegate processor.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void processCallMessage(CallMessage message, CallMessageResultHandler resultHandler) {
		Action action = message.getAction();
		CallMessageProcessor delegate = processors.get(action.getName());
		if ( delegate == null ) {
			BasicCallErrorMessage err = new BasicCallErrorMessage(message.getMessageId(),
					notImplementedErrorCode);
			resultHandler.handleCallMessageResult(message, null, err);
			return;
		}
		try {
			delegate.processCallMessage(message, resultHandler);
		} catch ( RuntimeException e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			log.error("CallMessageProcessor [{}] threw exception: {}", delegate, root.toString(), e);
			BasicCallErrorMessage err = new BasicCallErrorMessage(message.getMessageId(),
					internalErrorErrorCode,
					"CallMessageProcessor threw unexpected exception: " + root.getMessage(), null);
			resultHandler.handleCallMessageResult(message, null, err);
		}
	}

	/**
	 * Get the processors.
	 * 
	 * @return the processors, never {@literal null}
	 */
	public Map<String, CallMessageProcessor> getProcessors() {
		return processors;
	}

	/**
	 * Set the processors to use.
	 * 
	 * <p>
	 * The keys of this map are {@link ocpp.domain.Action#getName()} values,
	 * with associated {@link CallMessageProcessor} values for handling messages
	 * of that action.
	 * </p>
	 * 
	 * @param processors
	 *        the processors to use
	 * @throws IllegalArgumentException
	 *         if {@code processors} is {@literal null}
	 */
	public void setProcessors(Map<String, CallMessageProcessor> processors) {
		if ( processors == null ) {
			throw new IllegalArgumentException("The processors parameter must not be null.");
		}
		this.processors = processors;
	}

}
