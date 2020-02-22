/* ==================================================================
 * SystemUserConfig.java - 20/02/2020 7:53:41 pm
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.node.ocpp.domain.SystemUser;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;

/**
 * Configuration object for {@link SystemUser}.
 * 
 * @author matt
 * @version 1.0
 */
public class SystemUserConfig implements Identity<Long> {

	private Long id;
	private String username;
	private String password;
	private List<String> allowedChargePoints;

	/**
	 * Constructor.
	 */
	public SystemUserConfig() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param auth
	 *        the authorization entity to copy values from
	 */
	public SystemUserConfig(SystemUser entity) {
		super();
		if ( entity == null ) {
			return;
		}
		setId(entity.getId());
		setUsername(entity.getUsername());
		setPassword(entity.getPassword());
		Set<String> allowed = entity.getAllowedChargePoints();
		if ( allowed != null ) {
			setAllowedChargePoints(new ArrayList<>(allowed));
		}
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
		results.add(new BasicTextFieldSettingSpecifier(prefix + "username", username));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "password", "", true));

		// charging periods list
		List<String> allowed = getAllowedChargePoints();
		results.add(SettingsUtil.dynamicListSettingSpecifier(prefix + "allowedChargePoints", allowed,
				new SettingsUtil.KeyedListCallback<String>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(String value, int index,
							String key) {
						return Collections.<SettingSpecifier> singletonList(
								new BasicTextFieldSettingSpecifier(key, value));
					}
				}));
		return results;
	}

	@Override
	public int compareTo(Long o) {
		return id.compareTo(o);
	}

	@Override
	public Long getId() {
		return id;
	}

	/**
	 * Set the ID.
	 * 
	 * @param id
	 *        the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Get the username.
	 * 
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the username.
	 * 
	 * @param username
	 *        the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Get the password.
	 * 
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the password.
	 * 
	 * @param password
	 *        the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Get the allowed charge point IDs.
	 * 
	 * @return the charge point IDs
	 */
	public List<String> getAllowedChargePoints() {
		return allowedChargePoints;
	}

	/**
	 * Set the allowed charge point IDs.
	 * 
	 * @param allowedChargePoints
	 *        the charge point IDs to set
	 */
	public void setAllowedChargePoints(List<String> allowedChargePoints) {
		this.allowedChargePoints = allowedChargePoints;
	}

	/**
	 * Get the count of allowed charge points.
	 * 
	 * @return the count
	 */
	public int getAllowedChargePointsCount() {
		List<String> list = getAllowedChargePoints();
		return (list != null ? list.size() : 0);
	}

	/**
	 * Adjust the number of configured allowed charge points.
	 * 
	 * @param count
	 *        the desired number of elements
	 */
	public void setAllowedChargePointsCount(int count) {
		List<String> list = getAllowedChargePoints();
		int currCount = (list != null ? list.size() : 0);
		if ( currCount == count ) {
			return;
		}
		while ( currCount < count ) {
			if ( list == null ) {
				list = new ArrayList<>(count);
				setAllowedChargePoints(list);
			}
			list.add("");
			currCount++;
		}
		while ( currCount > count ) {
			list.remove(--currCount);
		}
	}

}
