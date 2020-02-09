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

import java.util.UUID;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.node.ocpp.domain.ChargeSession;

/**
 * Data Access Object API for {@link ChargeSession} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargeSessionDao extends GenericDao<ChargeSession, UUID> {

}
