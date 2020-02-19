/* ==================================================================
 * OcppChargingProfileManager.java - 19/02/2020 1:31:47 pm
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
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import net.solarnetwork.node.ocpp.dao.ChargingProfileDao;
import net.solarnetwork.node.ocpp.domain.ChargingProfile;
import net.solarnetwork.node.settings.SettingSpecifier;

/**
 * Manage {@link ChargingProfile} entities via settings.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppChargingProfileManager
		extends BaseEntityManager<ChargingProfileDao, ChargingProfile, UUID, ChargingProfileConfig> {

	/**
	 * Constructor.
	 * 
	 * @param chargingProfileDao
	 *        the DAO to use
	 */
	public OcppChargingProfileManager(ChargingProfileDao chargingProfileDao) {
		super(chargingProfileDao);
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.v16.cs.controller.OcppChargingProfileManager";
	}

	@Override
	public String getDisplayName() {
		return "OCPP Charging Profile Manager";
	}

	@Override
	protected ChargingProfile createNewEntity(ChargingProfileConfig conf) {
		return new ChargingProfile(conf.getId(), Instant.now());
	}

	@Override
	protected ChargingProfile cloneEntity(ChargingProfile entity) {
		return new ChargingProfile(entity);
	}

	@Override
	protected void applyConfiguration(ChargingProfileConfig conf, ChargingProfile entity) {
		entity.setInfo(conf.getInfo());
	}

	@Override
	protected List<SettingSpecifier> settingsForConfiguration(ChargingProfileConfig entity, int index,
			String keyPrefix) {
		return entity.settings(keyPrefix, getMessageSource(), Locale.getDefault());
	}

	@Override
	protected Function<? super ChargingProfile, ? extends ChargingProfileConfig> mapToConfiguration() {
		return ChargingProfileConfig::new;
	}

	@Override
	protected ChargingProfileConfig createNewConfiguration() {
		return new ChargingProfileConfig();
	}

}
