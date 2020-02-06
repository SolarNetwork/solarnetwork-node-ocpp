/* ==================================================================
 * ChargePoint.java - 7/02/2020 7:56:29 am
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
import net.solarnetwork.dao.BasicStringEntity;

/**
 * A Charge Point entity.
 * 
 * <p>
 * The primary key used is the charge point identity.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class ChargePoint extends BasicStringEntity {

	private ChargePointInfo info;
	private RegistrationStatus registrationStatus;
	private boolean enabled;

	/**
	 * Constructor.
	 */
	public ChargePoint() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 */
	public ChargePoint(String id) {
		this(id, Instant.now());
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param created
	 *        the created date
	 */
	public ChargePoint(String id, Instant created) {
		super(id, created);
	}

	/**
	 * Get the Charge Point information.
	 * 
	 * @return the info
	 */
	public ChargePointInfo getInfo() {
		return info;
	}

	/**
	 * Set the Charge Point information.
	 * 
	 * @param info
	 *        the info to set
	 */
	public void setInfo(ChargePointInfo info) {
		this.info = info;
	}

	/**
	 * Get the enabled flag.
	 * 
	 * @return the enabled flag
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the enabled flag.
	 * 
	 * @param enabled
	 *        the enabled flag to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Get the registration status.
	 * 
	 * @return the registrationStatus
	 */
	public RegistrationStatus getRegistrationStatus() {
		return registrationStatus;
	}

	/**
	 * Set the registration status.
	 * 
	 * @param registrationStatus
	 *        the registrationStatus to set
	 */
	public void setRegistrationStatus(RegistrationStatus registrationStatus) {
		this.registrationStatus = registrationStatus;
	}

}
