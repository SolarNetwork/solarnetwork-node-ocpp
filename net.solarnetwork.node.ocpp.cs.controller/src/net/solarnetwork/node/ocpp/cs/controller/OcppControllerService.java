/* ==================================================================
 * OcppControllerService.java - 6/02/2020 5:18:34 pm
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

package net.solarnetwork.node.ocpp.cs.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.node.ocpp.dao.ChargePointDao;
import net.solarnetwork.node.ocpp.domain.ChargePoint;
import net.solarnetwork.node.ocpp.domain.ChargePointInfo;
import net.solarnetwork.node.ocpp.service.cs.ChargePointManager;
import net.solarnetwork.node.support.BaseIdentifiable;

/**
 * API for an OCPP local controller service.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppControllerService extends BaseIdentifiable implements ChargePointManager {

	private final ChargePointDao chargePointDao;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param chargePointDao
	 */
	public OcppControllerService(ChargePointDao chargePointDao) {
		super();
		this.chargePointDao = chargePointDao;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChargePoint registerChargePoint(ChargePointInfo chargePoint) {
		log.info("Charge Point registration received: {}", chargePoint);
		return null;
	}

}
