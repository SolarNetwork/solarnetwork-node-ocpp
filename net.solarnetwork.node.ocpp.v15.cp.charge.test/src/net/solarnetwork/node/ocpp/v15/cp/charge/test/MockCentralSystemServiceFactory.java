/* ==================================================================
 * MockCentralSystemServiceFactory.java - 11/06/2015 9:07:28 am
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

package net.solarnetwork.node.ocpp.v15.cp.charge.test;

import net.solarnetwork.node.ocpp.v15.cp.CentralSystemServiceFactory;
import ocpp.v15.cs.CentralSystemService;

/**
 * Mock implementation of {@link CentralSystemServiceFactory} to help testing.
 * 
 * @author matt
 * @version 2.0
 */
public class MockCentralSystemServiceFactory implements CentralSystemServiceFactory {

	private final String uid;
	private final String groupUID;
	private final CentralSystemService service;
	private final String chargeBoxIdentity;

	/**
	 * Constructor.
	 * 
	 * @param uid
	 *        the UID
	 * @param groupUid
	 *        the group UID
	 * @param service
	 *        the service
	 * @param chargeBoxIdentity
	 *        the identity
	 */
	public MockCentralSystemServiceFactory(String uid, String groupUid, CentralSystemService service,
			String chargeBoxIdentity) {
		super();
		this.uid = uid;
		this.groupUID = groupUid;
		this.service = service;
		this.chargeBoxIdentity = chargeBoxIdentity;
	}

	@Override
	public String getDisplayName() {
		return "Mock Central System";
	}

	@Override
	public String getUid() {
		return uid;
	}

	@Override
	public String getGroupUid() {
		return groupUID;
	}

	@Override
	public CentralSystemService service() {
		return service;
	}

	@Override
	public String chargeBoxIdentity() {
		return chargeBoxIdentity;
	}

	@Override
	public boolean isBootNotificationPosted() {
		return false;
	}

	@Override
	public boolean postBootNotification() {
		return false;
	}

}
