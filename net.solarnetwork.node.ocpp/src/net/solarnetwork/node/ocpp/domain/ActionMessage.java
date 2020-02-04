/* ==================================================================
 * ActionMessage.java - 4/02/2020 4:12:19 pm
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

package net.solarnetwork.node.ocpp.domain;

import ocpp.domain.Action;

/**
 * An action (verb) with a message (content).
 * 
 * <p>
 * This API is not specific to any OCPP protocol version, so that services can
 * be designed that support multiple versions.
 * </p>
 * 
 * @param <T>
 *        the message type
 * @param <R>
 *        the result type
 * @author matt
 * @version 1.0
 */
public interface ActionMessage<T, R> {

	/**
	 * Get the action (verb) to perform.
	 * 
	 * @return the action; never {@literal null}
	 */
	Action getAction();

	/**
	 * Get the message (content).
	 * 
	 * @return the message, or {@literal null}
	 */
	T getMessage();

	/**
	 * Get the class of the result of this action.
	 * 
	 * @return the result class
	 */
	Class<R> getResultClass();

}
