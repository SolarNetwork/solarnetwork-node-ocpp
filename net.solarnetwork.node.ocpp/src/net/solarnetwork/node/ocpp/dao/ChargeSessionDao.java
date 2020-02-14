/* ==================================================================
 * ChargeSessionDao.java - 10/02/2020 9:15:46 am
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

package net.solarnetwork.node.ocpp.dao;

import java.util.List;
import java.util.UUID;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.node.ocpp.domain.ChargeSession;
import net.solarnetwork.node.ocpp.domain.SampledValue;

/**
 * Data Access Object API for {@link ChargeSession} entities.
 * 
 * <p>
 * <b>Note:</b> the {@link #save(ChargeSession)} method is expected to generate
 * a unique {@link ChargeSession#getTransactionId()} value.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargeSessionDao extends GenericDao<ChargeSession, UUID> {

	/**
	 * Get an <em>incomplete</em> charge session for a given transaction ID. An
	 * <em>incomplete</em> session is one that has no {@code ended} date.
	 * 
	 * @param transactionId
	 *        the transaction ID to look for
	 * @return the first available incomplete charge session, or {@literal null}
	 *         if not found
	 */
	ChargeSession getIncompleteChargeSessionForTransaction(String chargePointId, int transactionId);

	/**
	 * Store one or more charge session readings.
	 * 
	 * @param readings
	 *        the readings to store
	 */
	void addReadings(Iterable<SampledValue> readings);

	/**
	 * Get all available readings for a given session.
	 * 
	 * <p>
	 * The readings will be ordered by date, context, and location, measurand,
	 * and phase.
	 * </p>
	 * 
	 * @param sessionId
	 *        the session ID to get the readings for
	 * @return the readings, or an empty list if none available
	 */
	List<SampledValue> findReadingsForSession(UUID sessionId);

}
