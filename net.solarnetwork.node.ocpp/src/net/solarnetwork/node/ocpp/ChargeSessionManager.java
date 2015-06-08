/* ==================================================================
 * ChargeSessionManager.java - 8/06/2015 7:04:34 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp;

/**
 * API for managing charge sessions. A <em>charge session</em> is the process of
 * charging an electric device, e.g. plugging the device into a socket,
 * authorizing the use of the socket with the OCPP central system, monitoring
 * the energy used while charging, and unplugging the device from the socket,
 * and finally confirming that charging is complete.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargeSessionManager {

	/**
	 * Initiate a new charge session.
	 * 
	 * @param idTag
	 *        The ID to request authorization with.
	 * @param socketId
	 *        The ID of the physical socket to enable
	 * @param reservationId
	 *        An optional reservation ID. The reservation will be considered
	 *        satisfied upon successful return from this method.
	 * @return A unique charge session ID.
	 */
	String initiateChargeSession(String idTag, String socketId, String reservationId);

	/**
	 * Complete a charge session. This method must be called at some point after
	 * {@link #initiateChargeSession(String, String, String)}, passing in the
	 * return value of that method as the {@code sessionId}.
	 * 
	 * @param idTag
	 *        The ID to request authorization with.
	 * @param sessionId
	 *        The session ID returned from a previous call to
	 *        {@link #initiateChargeSession(String, String, String)}.
	 */
	void completeChargeSession(String idTag, String sessionId);

}
