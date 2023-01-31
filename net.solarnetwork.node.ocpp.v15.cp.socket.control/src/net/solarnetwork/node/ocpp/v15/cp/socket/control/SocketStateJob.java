/* ==================================================================
 * ChargeSessionCleanerJob.java - 31/07/2016 7:23:08 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.v15.cp.socket.control;

import net.solarnetwork.util.ObjectUtils;

/**
 * Job to examine charge sessions and make sure their corresponding socket state
 * is valid.
 * 
 * @author matt
 * @version 3.0
 */
public class SocketStateJob implements Runnable {

	private final SimpleSocketManager socketManager;

	/**
	 * Constructor.
	 * 
	 * @param socketManager
	 *        the manager
	 */
	public SocketStateJob(SimpleSocketManager socketManager) {
		super();
		this.socketManager = ObjectUtils.requireNonNullArgument(socketManager, "socketManager");
	}

	@Override
	public void run() {
		socketManager.verifyAllSockets();
	}

}
