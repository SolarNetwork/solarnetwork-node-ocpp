/* ==================================================================
 * PurgePostedChargeSessionsJob.java - 19/04/2018 3:49:12 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.v15.cp.charge;

import java.util.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.node.ocpp.v15.cp.ChargeSessionManager;
import net.solarnetwork.util.ObjectUtils;

/**
 * Job to delete old charge sessions that have been uploaded.
 * 
 * @author matt
 * @version 2.0
 * @since 0.6
 */
public class PurgePostedChargeSessionsJob implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(PurgePostedChargeSessionsJob.class);

	private final ChargeSessionManager service;
	private final TransactionTemplate transactionTemplate;
	private final int minPurgeUploadedSessionDays;

	/**
	 * Constructor.
	 * 
	 * @param service
	 *        the service
	 * @param transactionTemplate
	 *        the transaction template
	 */
	public PurgePostedChargeSessionsJob(ChargeSessionManager service,
			TransactionTemplate transactionTemplate) {
		this(service, transactionTemplate, 1);
	}

	/**
	 * Constructor.
	 * 
	 * @param service
	 *        the service
	 * @param transactionTemplate
	 *        the transaction template
	 * @param minPurgeUploadedSessionDays
	 *        the minimum purge expiration days
	 */
	public PurgePostedChargeSessionsJob(ChargeSessionManager service,
			TransactionTemplate transactionTemplate, int minPurgeUploadedSessionDays) {
		super();
		this.service = ObjectUtils.requireNonNullArgument(service, "service");
		this.transactionTemplate = transactionTemplate;
		this.minPurgeUploadedSessionDays = minPurgeUploadedSessionDays;
	}

	@Override
	public void run() {
		if ( transactionTemplate != null ) {
			transactionTemplate.execute(new TransactionCallback<Object>() {

				@Override
				public Object doInTransaction(TransactionStatus status) {
					purgePostedChargeSessions();
					return null;
				}
			});
		} else {
			purgePostedChargeSessions();
		}
	}

	private void purgePostedChargeSessions() {
		log.debug("Looking for OCPP posted charge sessions older than {} to purge");
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, -minPurgeUploadedSessionDays);
		int result = service.deletePostedChargeSessions(c.getTime());
		log.info("Purged {} uploaded OCPP charge session at least {} days old", result,
				minPurgeUploadedSessionDays);
	}

}
