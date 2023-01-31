/* ==================================================================
 * PostActiveChargeSessionsMeterValuesJob.java - 26/03/2017 9:02:48 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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
 * Job to periodically post charge session meter values.
 * 
 * @author matt
 * @version 2.0
 */
public class PostActiveChargeSessionsMeterValuesJob implements Runnable {

	private static final Logger log = LoggerFactory
			.getLogger(PostActiveChargeSessionsMeterValuesJob.class);

	private final ChargeSessionManager service;
	private final TransactionTemplate transactionTemplate;

	/**
	 * Constructor.
	 * 
	 * @param service
	 *        the service.
	 * @param transactionTemplate
	 *        the transaction template
	 */
	public PostActiveChargeSessionsMeterValuesJob(ChargeSessionManager service,
			TransactionTemplate transactionTemplate) {
		super();
		this.service = ObjectUtils.requireNonNullArgument(service, "service");
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public void run() {
		if ( transactionTemplate != null ) {
			transactionTemplate.execute(new TransactionCallback<Object>() {

				@Override
				public Object doInTransaction(TransactionStatus arg0) {
					service.postActiveChargeSessionsMeterValues();
					return null;
				}
			});
		} else {
			service.postActiveChargeSessionsMeterValues();
		}
		log.debug("Completed posting active charge sessions meter values to OCPP central system");
	}

}
