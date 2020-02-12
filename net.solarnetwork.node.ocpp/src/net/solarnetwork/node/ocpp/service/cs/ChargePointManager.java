/* ==================================================================
 * ChargePointManager.java - 6/02/2020 7:38:10 pm
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

package net.solarnetwork.node.ocpp.service.cs;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.solarnetwork.domain.Identifiable;
import net.solarnetwork.node.ocpp.domain.ChargePoint;
import net.solarnetwork.node.ocpp.domain.ChargePointInfo;

/**
 * This API represents the set of functionality required by an OCPP Central
 * System to manage a set of Charge Point clients.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargePointManager extends Identifiable {

	/**
	 * Register (or re-register) a Charge Point.
	 * 
	 * <p>
	 * This method can be called by a Charge Point that wants to self-register,
	 * for example via a {@literal BootNotification} request, or by an
	 * administration tool to create a new Charge Point entity.
	 * </p>
	 * 
	 * @param info
	 *        the details to register
	 * @return the registered charge point
	 */
	ChargePoint registerChargePoint(ChargePointInfo info);

	/**
	 * Get a complete set of Charge Point identifiers that are available, or
	 * otherwise know to this manager.
	 * 
	 * @return the set of available charge point identifiers, never
	 *         {@literal null}
	 */
	Set<String> availableChargePointsIds();

	/**
	 * Test if a Charge Point is available, or otherwise known to this manager.
	 * 
	 * @param chargePointId
	 *        the Charge Point ID to query
	 * @return {@literal true} if this manager is aware of the given
	 *         {@code clientId} and should be able to send messages to it
	 */
	boolean isChargePointAvailable(String chargePointId);

	/**
	 * Set the connector enabled state for a given connector ID.
	 * 
	 * @param chargePointId
	 *        the ID of the Charge Point with the connector to adjust
	 * @param connectorId
	 *        the ID of the connector to adjust; connector IDs start at
	 *        {@literal 1} and increment by one for each additional connector on
	 *        a Charge Point
	 * @param enabled
	 *        {@literal true} to make the connector available for use,
	 *        {@literal false} to disable the connector
	 * @return {@literal true} if the state was set
	 */
	CompletableFuture<Boolean> adjustConnectorEnabledState(String chargePointId, int connectorId,
			boolean enabled);

}
