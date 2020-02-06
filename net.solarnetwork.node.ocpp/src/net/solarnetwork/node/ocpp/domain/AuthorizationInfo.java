/* ==================================================================
 * AuthorizationInfo.java - 6/02/2020 7:23:43 pm
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

import java.time.Instant;

/**
 * Charge Point authorization information.
 * 
 * @author matt
 * @version 1.0
 */
public class AuthorizationInfo {

	private final String idTag;
	private final String status;
	private final Instant expiryDate;
	private final String parentIdTag;

	/**
	 * Constructor.
	 * 
	 * @param idTag
	 *        the ID tag value, e.g. RFID ID
	 * @param status
	 *        the associated OCCP status, e.g. {@literal Accepted}
	 * @param expiryDate
	 *        the expiration date
	 * @param parentIdTag
	 *        a parent ID tag
	 */
	public AuthorizationInfo(String idTag, String status, Instant expiryDate, String parentIdTag) {
		super();
		this.idTag = idTag;
		this.status = status;
		this.expiryDate = expiryDate;
		this.parentIdTag = parentIdTag;
	}

	public String getIdTag() {
		return idTag;
	}

	public String getStatus() {
		return status;
	}

	public Instant getExpiryDate() {
		return expiryDate;
	}

	public String getParentIdTag() {
		return parentIdTag;
	}

}
