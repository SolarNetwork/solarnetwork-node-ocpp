/* ==================================================================
 * PostOfflineChargeSessionsJob.java - 16/06/2015 7:42:31 pm
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

package net.solarnetwork.node.ocpp.v15.cp.charge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.node.ocpp.v15.cp.ChargeSessionManager;
import net.solarnetwork.util.ObjectUtils;

/**
 * Job to periodically look for offline charge sessions that need to be posted
 * to the central system.
 * 
 * @author matt
 * @version 3.0
 */
public class PostOfflineChargeSessionsJob implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(PostOfflineChargeSessionsJob.class);

	private final ChargeSessionManager service;
	private final TransactionTemplate transactionTemplate;
	private final int maximum;

	/**
	 * Constructor.
	 * 
	 * @param service
	 *        the service.
	 * @param transactionTemplate
	 *        the transaction template
	 */
	public PostOfflineChargeSessionsJob(ChargeSessionManager service,
			TransactionTemplate transactionTemplate) {
		this(service, transactionTemplate, 5);
	}

	/**
	 * Constructor.
	 * 
	 * @param service
	 *        the service.
	 * @param transactionTemplate
	 *        the transaction template
	 * @param maximum
	 *        the maximum to post
	 */
	public PostOfflineChargeSessionsJob(ChargeSessionManager service,
			TransactionTemplate transactionTemplate, int maximum) {
		super();
		this.service = ObjectUtils.requireNonNullArgument(service, "service");
		this.transactionTemplate = transactionTemplate;
		this.maximum = maximum;
	}

	@Override
	public void run() {
		if ( transactionTemplate != null ) {
			transactionTemplate.execute(new TransactionCallback<Object>() {

				@Override
				public Object doInTransaction(TransactionStatus status) {
					postCompletedOfflineSessions();
					return null;
				}
			});
		} else {
			postCompletedOfflineSessions();
		}
	}

	private void postCompletedOfflineSessions() {
		final int posted = service.postCompleteOfflineSessions(maximum);
		log.info("{} completed offline charge sessions posted to OCPP central system", posted);
	}

}
