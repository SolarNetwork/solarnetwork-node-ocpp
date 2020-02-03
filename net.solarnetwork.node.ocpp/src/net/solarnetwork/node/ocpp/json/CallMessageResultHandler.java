/* ==================================================================
 * CallMessageResultHandler.java - 2/02/2020 5:48:00 pm
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

import ocpp.json.CallErrorMessage;
import ocpp.json.CallMessage;
import ocpp.json.CallResultMessage;

/**
 * API for handling the result of a {@link CallMessage}.
 * 
 * @author matt
 * @version 1.0
 */
@FunctionalInterface
public interface CallMessageResultHandler {

	/**
	 * Handle a {@link CallMessage} result.
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
	boolean handleCallMessageResult(CallMessage message, CallResultMessage result,
			CallErrorMessage error);

}
