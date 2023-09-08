/* ==================================================================
 * OcppRegistrationManager.java - 13/02/2020 11:25:38 am
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
import java.util.function.Function;
import net.solarnetwork.ocpp.dao.ChargePointDao;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Manager for Charge Point registrations.
 * 
 * @author matt
 * @version 2.0
 */
public class OcppRegistrationManager
		extends BaseEntityManager<ChargePointDao, ChargePoint, Long, ChargePointConfig> {

	/**
	 * Constructor.
	 * 
	 * @param chargePointDao
	 *        the DAO to manage charge points with
	 */
	public OcppRegistrationManager(ChargePointDao chargePointDao) {
		super(chargePointDao);
	}

	@Override
	protected ChargePoint createNewEntity(ChargePointConfig conf) {
		return new ChargePoint(conf.getId(), Instant.now());
	}

	@Override
	protected ChargePoint cloneEntity(ChargePoint entity) {
		return new ChargePoint(entity);
	}

	@Override
	protected void applyConfiguration(ChargePointConfig conf, ChargePoint entity) {
		entity.setEnabled(conf.isEnabled());
		entity.setRegistrationStatus(conf.getRegistrationStatus());
		if ( conf.getInfo() != null ) {
			if ( conf.getInfo().getId() != null ) {
				entity.getInfo().setId(conf.getInfo().getId());
			}
			if ( conf.getInfo().getChargePointVendor() != null ) {
				entity.getInfo().setChargePointVendor(conf.getInfo().getChargePointVendor());
			}
			if ( conf.getInfo().getChargePointModel() != null ) {
				entity.getInfo().setChargePointModel(conf.getInfo().getChargePointModel());
			}
		}
	}

	@Override
	protected boolean shouldIgnoreConfiguration(ChargePointConfig conf) {
		return conf == null || conf.getInfo() == null || conf.getInfo().getId() == null
				|| conf.getInfo().getChargePointVendor() == null
				|| conf.getInfo().getChargePointModel() == null;
	}

	@Override
	protected Long saveConfiguration(ChargePointConfig conf, ChargePoint entity) {
		Long pk = super.saveConfiguration(conf, entity);
		conf.setId(pk);
		if ( conf.getCreated() == null ) {
			ChargePoint saved = dao.get(pk);
			conf.setCreated(saved.getCreated());
		}
		return pk;
	}

	@Override
	protected List<SettingSpecifier> settingsForConfiguration(ChargePointConfig conf, int index,
			String keyPrefix) {
		return conf.settings(getMessageSource(), Locale.getDefault(), keyPrefix);
	}

	@Override
	protected Function<? super ChargePoint, ? extends ChargePointConfig> mapToConfiguration() {
		return ChargePointConfig::new;
	}

	@Override
	protected ChargePointConfig createNewConfiguration() {
		return new ChargePointConfig();
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.ocpp.v16.cs.controller.OcppRegistrationManager";
	}

	@Override
	public String getDisplayName() {
		return "OCPP ChargePoint Manager";
	}

}
