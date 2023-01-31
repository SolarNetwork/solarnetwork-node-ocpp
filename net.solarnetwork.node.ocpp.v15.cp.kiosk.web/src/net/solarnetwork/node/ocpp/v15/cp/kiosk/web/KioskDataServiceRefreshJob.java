/* ==================================================================
 * KioskDataServiceRefreshJob.java - 23/10/2016 7:06:40 AM
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

package net.solarnetwork.node.ocpp.v15.cp.kiosk.web;

import net.solarnetwork.util.ObjectUtils;

/**
 * Job to run to refresh the kiosk data model.
 * 
 * @author matt
 * @version 1.0
 */
public class KioskDataServiceRefreshJob implements Runnable {

	private final KioskDataService dataService;

	/**
	 * Constructor.
	 * 
	 * @param dataService
	 *        the service
	 */
	public KioskDataServiceRefreshJob(KioskDataService dataService) {
		super();
		this.dataService = ObjectUtils.requireNonNullArgument(dataService, "dataService");
	}

	@Override
	public void run() {
		dataService.refreshKioskData();
	}

}
