/* ==================================================================
 * PendingCallMessage.java - 3/02/2020 9:57:16 am
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

import net.solarnetwork.node.ocpp.json.CallMessageResultHandler;
import ocpp.json.CallMessage;

/**
 * A "pending" call message, which is waiting for a corresponding result message
 * to be received.
 * 
 * @author matt
 * @version 1.0
 */
public class PendingCallMessage {

	private final long date;
	private final CallMessage message;
	private final CallMessageResultHandler handler;

	/**
	 * Constructor.
	 * 
	 * @param date
	 *        the date, in milliseconds since the epoch
	 * @param message
	 *        the message
	 * @param handler
	 *        the handler for the message result
	 * @throws IllegalArgumentException
	 *         if any parmeter is {@literal null}
	 */
	public PendingCallMessage(long date, CallMessage message, CallMessageResultHandler handler) {
		super();
		this.date = date;
		if ( message == null ) {
			throw new IllegalArgumentException("The message parameter must not be null.");
		}
		this.message = message;
		if ( handler == null ) {
			throw new IllegalArgumentException("The handler parameter must not be null.");
		}
		this.handler = handler;
	}

	/**
	 * Get the message date.
	 * 
	 * @return the date
	 */
	public long getDate() {
		return date;
	}

	/**
	 * Get the message.
	 * 
	 * @return the message; never {@literal null}
	 */
	public CallMessage getMessage() {
		return message;
	}

	/**
	 * Get the result handler.
	 * 
	 * @return the handler; never {@literal null}
	 */
	public CallMessageResultHandler getHandler() {
		return handler;
	}

}
