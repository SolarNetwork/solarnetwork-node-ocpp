/* ==================================================================
 * Authorization.java - 9/02/2020 1:57:16 pm
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
 * An authorization entity.
 * 
 * <p>
 * The primary key used is the external ID value, e.g. RFID tag ID.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class Authorization extends BasicStringEntity {

	private boolean enabled;
	private Instant expiryDate;
	private String parentId;

	/**
	 * Constructor.
	 */
	public Authorization() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 */
	public Authorization(String id) {
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
	public Authorization(String id, Instant created) {
		super(id, created);
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
	 * Get the expiration date.
	 * 
	 * @return the expiration date, or {@literal null} for no expiration
	 */
	public Instant getExpiryDate() {
		return expiryDate;
	}

	/**
	 * Set the expiration date.
	 * 
	 * @param expiryDate
	 *        the expiration date to set, or {@literal null} for no expiration
	 */
	public void setExpiryDate(Instant expiryDate) {
		this.expiryDate = expiryDate;
	}

	/**
	 * Get the ID of a parent authorization.
	 * 
	 * @return the parent ID, or {@literal null} if there is no parent
	 */
	public String getParentId() {
		return parentId;
	}

	/**
	 * Set the ID of a parent authorization.
	 * 
	 * @param parentId
	 *        the parent ID to set, or {@literal null} if there is no parent
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

}
