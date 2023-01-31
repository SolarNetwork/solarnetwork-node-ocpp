/* ==================================================================
 * PurgeExpiredAuthorizationsJob.java - 8/06/2015 7:34:58 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.v15.cp.dao;

import java.util.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.ocpp.v15.cp.AuthorizationDao;
import net.solarnetwork.util.ObjectUtils;

/**
 * Job to purge expired authorizations by calling
 * {@link AuthorizationDao#deleteExpiredAuthorizations(java.util.Date)}. The
 * maximum expired date to delete is derived from
 * {@link #getMinPurgeExpiredAuthorizationDays()}.
 * 
 * @author matt
 * @version 3.0
 */
public class PurgeExpiredAuthorizationsJob implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(PurgeExpiredAuthorizationsJob.class);

	private final AuthorizationDao authorizationDao;
	private final int minPurgeExpiredAuthorizationDays;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@code minPurgeExpiredAuthorizationDays} will will be set to
	 * {@literal 1}.
	 * </p>
	 * 
	 * @param authorizationDao
	 *        the DAO to use
	 */
	public PurgeExpiredAuthorizationsJob(AuthorizationDao authorizationDao) {
		this(authorizationDao, 1);
	}

	/**
	 * Constructor.
	 * 
	 * @param authorizationDao
	 *        the DAO to use
	 * @param minPurgeExpiredAuthorizationDays
	 *        the minimum days old to expire
	 */
	public PurgeExpiredAuthorizationsJob(AuthorizationDao authorizationDao,
			int minPurgeExpiredAuthorizationDays) {
		super();
		this.authorizationDao = ObjectUtils.requireNonNullArgument(authorizationDao, "authorizationDao");
		this.minPurgeExpiredAuthorizationDays = minPurgeExpiredAuthorizationDays;
	}

	@Override
	public void run() {
		try {
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -minPurgeExpiredAuthorizationDays);
			int result = authorizationDao.deleteExpiredAuthorizations(c.getTime());
			log.info("Purged {} expired OCPP authorizations {} days old", result,
					minPurgeExpiredAuthorizationDays);
		} catch ( Exception e ) {
			log.error("Error deleting expired OCPP authorizations older than {} days: {}",
					minPurgeExpiredAuthorizationDays, e.toString());
		}
	}

}
