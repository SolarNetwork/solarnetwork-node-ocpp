/* ==================================================================
 * OcppAuthorizationManager.java - 11/02/2020 7:45:41 pm
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
import java.util.List;
import java.util.function.Function;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.ocpp.dao.AuthorizationDao;
import net.solarnetwork.ocpp.domain.Authorization;

/**
 * Manage {@link Authorization} entities via settings.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppAuthorizationManager
		extends BaseEntityManager<AuthorizationDao, Authorization, Long, AuthorizationConfig> {

	/**
	 * Constructor.
	 * 
	 * @param authorizationDao
	 *        the DAO to manage authorizations with
	 */
	public OcppAuthorizationManager(AuthorizationDao authorizationDao) {
		super(authorizationDao);
	}

	@Override
	protected Authorization createNewEntity(AuthorizationConfig conf) {
		return new Authorization(conf.getId(), Instant.now());
	}

	@Override
	protected Authorization cloneEntity(Authorization entity) {
		return new Authorization(entity);
	}

	@Override
	protected void applyConfiguration(AuthorizationConfig conf, Authorization entity) {
		entity.setEnabled(conf.isEnabled());
		entity.setExpiryDate(conf.getExpiryDate());
		entity.setParentId(conf.getParentId());
	}

	@Override
	protected List<SettingSpecifier> settingsForConfiguration(AuthorizationConfig entity, int index,
			String keyPrefix) {
		return entity.settings(keyPrefix);
	}

	@Override
	protected Function<? super Authorization, ? extends AuthorizationConfig> mapToConfiguration() {
		return AuthorizationConfig::new;
	}

	@Override
	protected AuthorizationConfig createNewConfiguration() {
		return new AuthorizationConfig();
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.v16.cs.controller.OcppAuthorizationManager";
	}

	@Override
	public String getDisplayName() {
		return "OCPP Authorization Manager";
	}

}
