/* ==================================================================
 * OcppSystemUserManager.java - 20/02/2020 8:15:26 pm
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.solarnetwork.node.ocpp.dao.SystemUserDao;
import net.solarnetwork.node.ocpp.domain.SystemUser;
import net.solarnetwork.node.settings.SettingSpecifier;

/**
 * Manager for system users.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppSystemUserManager
		extends BaseEntityManager<SystemUserDao, SystemUser, Long, SystemUserConfig> {

	/**
	 * Constructor.
	 * 
	 * @param dao
	 *        the DAO to use
	 */
	public OcppSystemUserManager(SystemUserDao dao) {
		super(dao);
		setFindAllSorts(null); // assume default DAO sorts by created,id,idx
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.v16.cs.controller.OcppSystemUserManager";
	}

	@Override
	public String getDisplayName() {
		return "OCPP System User Manager";
	}

	@Override
	protected SystemUser createNewEntity(SystemUserConfig conf) {
		return new SystemUser(conf.getId(), Instant.now());
	}

	@Override
	protected SystemUser cloneEntity(SystemUser entity) {
		return new SystemUser(entity);
	}

	@Override
	protected void applyConfiguration(SystemUserConfig conf, SystemUser entity) {
		entity.setUsername(conf.getUsername());
		if ( conf.getPassword() != null && !conf.getPassword().isEmpty() ) {
			entity.setPassword(conf.getPassword());
		}
		Set<String> allowed = null;
		if ( conf.getAllowedChargePoints() != null ) {
			allowed = new LinkedHashSet<>(conf.getAllowedChargePoints());
		}
		entity.setAllowedChargePoints(allowed);
	}

	@Override
	protected boolean shouldIgnoreConfiguration(SystemUserConfig conf) {
		return conf == null;
	}

	@Override
	protected Long saveConfiguration(SystemUserConfig conf, SystemUser entity) {
		Long pk = super.saveConfiguration(conf, entity);
		conf.setId(pk);
		return pk;
	}

	@Override
	protected List<SettingSpecifier> settingsForConfiguration(SystemUserConfig conf, int index,
			String keyPrefix) {
		return conf.settings(keyPrefix);
	}

	@Override
	protected Function<? super SystemUser, ? extends SystemUserConfig> mapToConfiguration() {
		return SystemUserConfig::new;
	}

	@Override
	protected SystemUserConfig createNewConfiguration() {
		return new SystemUserConfig();
	}

}
