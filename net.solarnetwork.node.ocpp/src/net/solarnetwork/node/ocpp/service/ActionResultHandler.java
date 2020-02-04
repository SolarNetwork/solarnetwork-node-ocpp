/* ==================================================================
 * ActionResultHandler.java - 4/02/2020 4:10:56 pm
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

package net.solarnetwork.node.ocpp.service;

import ocpp.domain.Action;
import ocpp.json.CallErrorMessage;
import ocpp.json.CallResultMessage;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface ActionResultHandler {

	/**
	 * Handle an {@link Action} result.
	 * 
	 * @param message
	 *        the source message the result is for
	 * @param result
	 *        the successful result, or {@literal null} if {@code error} is
	 *        provided
	 * @param error
	 *        the error result, or {@literal null} if {@code result} is provided
	 * @return {@literal true} if the result was handled, {@literal false}
	 *         otherwise
	 */
	boolean handleCallMessageResult(Action action, CallResultMessage result, CallErrorMessage error);

}
