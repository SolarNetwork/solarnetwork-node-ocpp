/* ==================================================================
 * AuthorizationConfig.java - 11/02/2020 8:06:03 pm
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

package net.solarnetwork.node.ocpp.v16.cs.controller;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.ocpp.domain.Authorization;
import net.solarnetwork.util.DateUtils;

/**
 * Configuration for an {@link Authorization}.
 * 
 * @author matt
 * @version 1.0
 */
public class AuthorizationConfig implements Identity<String> {

	private String id;
	private boolean enabled;
	private Instant expiryDate;
	private String parentId;

	/**
	 * Constructor.
	 */
	public AuthorizationConfig() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param auth
	 *        the authorization entity to copy values from
	 */
	public AuthorizationConfig(Authorization auth) {
		super();
		if ( auth == null ) {
			return;
		}
		setId(auth.getId());
		setEnabled(auth.isEnabled());
		setExpiryDate(auth.getExpiryDate());
		setParentId(auth.getParentId());
	}

	/**
	 * Get the setting specifiers for an {@link AuthorizationConfig}.
	 * 
	 * @param prefix
	 *        the prefix to use for each setting key
	 * @return the settings
	 */
	public List<SettingSpecifier> settings(String prefix) {
		if ( prefix == null ) {
			prefix = "";
		}
		List<SettingSpecifier> results = new ArrayList<>(4);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "id", id));
		results.add(new BasicToggleSettingSpecifier(prefix + "enabled", enabled));
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "expiryDateValue", getExpiryDateValue()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "parentId", parentId));
		return results;
	}

	@Override
	public int compareTo(String o) {
		return id.compareTo(o);
	}

	@Override
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *        the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled
	 *        the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @return the expiryDate
	 */
	public Instant getExpiryDate() {
		return expiryDate;
	}

	/**
	 * @param expiryDate
	 *        the expiryDate to set
	 */
	public void setExpiryDate(Instant expiryDate) {
		this.expiryDate = expiryDate;
	}

	/**
	 * Get the expiry date as a formatted instant.
	 * 
	 * @return the expiry date, as an ISO 8601 formatted string
	 */
	public String getExpiryDateValue() {
		Instant ts = getExpiryDate();
		return (ts != null ? DateUtils.ISO_DATE_OPT_TIME_ALT_LOCAL.format(ts) : null);
	}

	/**
	 * Set the expiry date as an ISO 8601 formatted timestamp.
	 * 
	 * @param value
	 *        the date string
	 */
	public void setExpiryDateValue(String value) {
		Instant ts = null;
		if ( value != null ) {
			ZonedDateTime date = DateUtils.parseIsoAltTimestamp(value, ZoneId.systemDefault());
			if ( date != null ) {
				ts = date.toInstant();
			}
		}
		setExpiryDate(ts);
	}

	/**
	 * @return the parentId
	 */
	public String getParentId() {
		return parentId;
	}

	/**
	 * @param parentId
	 *        the parentId to set
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

}
