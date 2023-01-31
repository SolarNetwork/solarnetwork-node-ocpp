/* ==================================================================
 * HeartbeatJob.java - 6/06/2015 5:08:48 pm
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

package net.solarnetwork.node.ocpp.v15.cp.impl;

import javax.xml.ws.WebServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.ocpp.v15.cp.CentralSystemServiceFactory;
import net.solarnetwork.util.ObjectUtils;
import ocpp.v15.cs.CentralSystemService;
import ocpp.v15.cs.HeartbeatRequest;
import ocpp.v15.cs.HeartbeatResponse;

/**
 * Job to post the {@link HeartbeatRequest} to let the OCPP system know the node
 * is alive and has network connectivity.
 * 
 * @author matt
 * @version 3.0
 */
public class HeartbeatJob implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(HeartbeatJob.class);

	private final CentralSystemServiceFactory service;

	/**
	 * Constructor.
	 * 
	 * @param service
	 *        the service factory to use
	 */
	public HeartbeatJob(CentralSystemServiceFactory service) {
		super();
		this.service = ObjectUtils.requireNonNullArgument(service, "service");
	}

	@Override
	public void run() {
		try {
			if ( !service.isBootNotificationPosted() ) {
				service.postBootNotification();
				return;
			}

			CentralSystemService client = service.service();
			if ( client == null ) {
				log.warn("No CentralSystemService avaialble, cannot post heartbeat message.");
				return;
			}
			HeartbeatRequest req = new HeartbeatRequest();
			HeartbeatResponse res = client.heartbeat(req, service.chargeBoxIdentity());
			log.info("OCPP heartbeat response: {}", res == null ? null : res.getCurrentTime());
		} catch ( WebServiceException e ) {
			log.warn("Error communicating with OCPP central system: {}", e.getMessage());
		}
	}

}
