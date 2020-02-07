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

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.node.ocpp.dao.ChargePointDao;
import net.solarnetwork.node.ocpp.domain.ChargePoint;
import net.solarnetwork.node.ocpp.domain.ChargePointInfo;
import net.solarnetwork.node.ocpp.domain.RegistrationStatus;
import net.solarnetwork.node.ocpp.service.cs.ChargePointManager;
import net.solarnetwork.node.support.BaseIdentifiable;

/**
 * API for an OCPP local controller service.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppControllerService extends BaseIdentifiable implements ChargePointManager {

	/** The default {@code initialRegistrationStatus} value. */
	public static final RegistrationStatus DEFAULT_INITIAL_REGISTRATION_STATUS = RegistrationStatus.Pending;

	private final ChargePointDao chargePointDao;
	private RegistrationStatus initialRegistrationStatus;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param chargePointDao
	 */
	public OcppControllerService(ChargePointDao chargePointDao) {
		super();
		this.chargePointDao = chargePointDao;
		this.initialRegistrationStatus = DEFAULT_INITIAL_REGISTRATION_STATUS;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChargePoint registerChargePoint(ChargePointInfo info) {
		log.info("Charge Point registration received: {}", info);

		if ( info == null || info.getId() == null ) {
			throw new IllegalArgumentException("The ChargePoint ID must be provided.");
		}

		ChargePoint cp = chargePointDao.get(info.getId());
		if ( cp == null ) {
			cp = registerNewChargePoint(info);
		} else if ( cp.isEnabled() ) {
			cp = updateChargePointInfo(cp, info);
		}

		return cp;
	}

	private ChargePoint registerNewChargePoint(ChargePointInfo info) {
		log.info("Registering new ChargePoint {}", info);
		ChargePoint cp = new ChargePoint(info.getId(), Instant.now());
		cp.setEnabled(true);
		cp.setRegistrationStatus(getInitialRegistrationStatus());
		cp.setInfo(info);
		chargePointDao.save(cp);
		return cp;
	}

	private ChargePoint updateChargePointInfo(ChargePoint cp, ChargePointInfo info) {
		assert cp != null && cp.getInfo() != null;
		if ( cp.getInfo().isSameAs(info) ) {
			log.info("ChargePoint registration info is unchanged: {}", info);
		} else {
			log.info("Updating ChargePoint registration info {} -> {}", cp.getInfo(), info);
			cp.setInfo(info);
			chargePointDao.save(cp);
		}
		return cp;
	}

	/**
	 * Get the initial {@link RegistrationStatus} to use for newly registered
	 * charge points.
	 * 
	 * @return the status, never {@literal null}
	 */
	public RegistrationStatus getInitialRegistrationStatus() {
		return initialRegistrationStatus;
	}

	/**
	 * Set the initial {@link RegistrationStatus} to use for newly registered
	 * charge points.
	 * 
	 * @param initialRegistrationStatus
	 *        the status to set
	 * @throws IllegalArgumentException
	 *         if {@code initialRegistrationStatus} is {@literal null}
	 */
	public void setInitialRegistrationStatus(RegistrationStatus initialRegistrationStatus) {
		if ( initialRegistrationStatus == null ) {
			throw new IllegalArgumentException(
					"The initialRegistrationStatus parameter must not be null.");
		}
		this.initialRegistrationStatus = initialRegistrationStatus;
	}

}
