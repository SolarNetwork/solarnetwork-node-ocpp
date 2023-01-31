/* ==================================================================
 * CloseCompletedChargeSessionsJob.java - 24/03/2017 10:13:38 AM
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

import java.util.List;
import java.util.ListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.node.ocpp.v15.cp.ChargeSession;
import net.solarnetwork.node.ocpp.v15.cp.ChargeSessionManager;
import net.solarnetwork.node.ocpp.v15.cp.ChargeSessionMeterReading;
import net.solarnetwork.util.ObjectUtils;
import ocpp.v15.cs.Measurand;

/**
 * Job to periodically look for active charge sessions that appear to have
 * finished because of a lack of power being drawn on the associated socket.
 * 
 * @author matt
 * @version 2.0
 */
public class CloseCompletedChargeSessionsJob implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(CloseCompletedChargeSessionsJob.class);

	private final ChargeSessionManager service;
	private final TransactionTemplate transactionTemplate;
	private final long maxAgeLastReading;
	private final int readingEnergyCount;
	private final long maxEnergy;

	/**
	 * Constructor.
	 * 
	 * @param service
	 *        the service.
	 * @param transactionTemplate
	 *        the transaction template
	 */
	public CloseCompletedChargeSessionsJob(ChargeSessionManager service,
			TransactionTemplate transactionTemplate) {
		this(service, transactionTemplate, 15 * 60 * 1000L);
	}

	/**
	 * Constructor.
	 * 
	 * @param service
	 *        the service.
	 * @param transactionTemplate
	 *        the transaction template
	 * @param maxAgeLastReading
	 *        the maximum age in milliseconds from the last meter reading
	 *        captured for that session (or the date the session started, if no
	 *        readings are available); if this threshold is passed then the
	 *        session will be closed
	 * @param readingEnergyCount
	 *        the number of meter readings to consider when calculating the
	 *        effective energy drawn on the socket
	 * @param maxEnergy
	 *        the maximum energy, in Wh, a charge session can draw over
	 *        {@code readingEnergyCount} readings to be considered for closing;
	 *        if the energy drawn is higher than this, the session will not be
	 *        closed
	 */
	public CloseCompletedChargeSessionsJob(ChargeSessionManager service,
			TransactionTemplate transactionTemplate, long maxAgeLastReading) {
		this(service, transactionTemplate, maxAgeLastReading, 5, 5L);
	}

	/**
	 * Constructor.
	 * 
	 * @param service
	 *        the service.
	 * @param transactionTemplate
	 *        the transaction template
	 * @param maxAgeLastReading
	 *        the maximum age in milliseconds from the last meter reading
	 *        captured for that session (or the date the session started, if no
	 *        readings are available); if this threshold is passed then the
	 *        session will be closed
	 * @param readingEnergyCount
	 *        the number of meter readings to consider when calculating the
	 *        effective energy drawn on the socket
	 * @param maxEnergy
	 *        the maximum energy, in Wh, a charge session can draw over
	 *        {@code readingEnergyCount} readings to be considered for closing;
	 *        if the energy drawn is higher than this, the session will not be
	 *        closed
	 */
	public CloseCompletedChargeSessionsJob(ChargeSessionManager service,
			TransactionTemplate transactionTemplate, long maxAgeLastReading, int readingEnergyCount,
			long maxEnergy) {
		super();
		this.service = ObjectUtils.requireNonNullArgument(service, "service");
		this.transactionTemplate = transactionTemplate;
		this.maxAgeLastReading = maxAgeLastReading;
		this.readingEnergyCount = readingEnergyCount;
		this.maxEnergy = maxEnergy;
	}

	@Override
	public void run() {
		if ( service == null ) {
			log.warn(
					"No ChargeSessionManager available, cannot close active charge sessions that appear to be completed");
			return;
		}
		if ( transactionTemplate != null ) {
			transactionTemplate.execute(new TransactionCallback<Object>() {

				@Override
				public Object doInTransaction(TransactionStatus status) {
					closeCompletedChargeSessions();
					return null;
				}
			});
		} else {
			closeCompletedChargeSessions();
		}
	}

	private void closeCompletedChargeSessions() {
		log.debug("Looking for OCPP active charge sessions that appear to be completed");
		for ( String socketId : service.availableSocketIds() ) {
			ChargeSession session = service.activeChargeSession(socketId);
			if ( session != null ) {
				List<ChargeSessionMeterReading> readings = service
						.meterReadingsForChargeSession(session.getSessionId());
				boolean close = false;
				if ( (readings == null || readings.isEmpty()) && session.getCreated() != null
						&& (session.getCreated().getTime() + maxAgeLastReading) < System
								.currentTimeMillis() ) {
					log.info(
							"OCCP charge session {} on socket {} has not recorded any readings since session started at {}; closing session",
							session.getSessionId(), socketId, session.getCreated());
					close = true;
				} else if ( readings != null && !readings.isEmpty() ) {
					ChargeSessionMeterReading reading = readings.get(readings.size() - 1);
					if ( reading != null && reading.getTs() != null && reading.getTs().getTime()
							+ maxAgeLastReading < System.currentTimeMillis() ) {
						log.info(
								"OCCP charge session {} on socket {} has not recorded any readings since {}; closing session",
								session.getSessionId(), socketId, reading.getTs());
						close = true;
					} else if ( readingEnergyCount > 0 && readings.size() >= readingEnergyCount ) {
						// look to see if the energy drawn over the last few readings is about 0, meaning the battery is charged
						// we assume readings are taken at regular intervals
						ListIterator<ChargeSessionMeterReading> itr = readings
								.listIterator(readings.size());
						int count = 0;
						long whEnd = 0;
						long whStart = 0;
						while ( itr.hasPrevious() && count < readingEnergyCount ) {
							reading = itr.previous();
							if ( reading.getMeasurand() == Measurand.ENERGY_ACTIVE_IMPORT_REGISTER ) {
								count += 1;
								if ( count == 1 ) {
									whEnd = Long.valueOf(reading.getValue());
								} else if ( count == readingEnergyCount ) {
									whStart = Long.valueOf(reading.getValue());
									break;
								}
							}
						}
						if ( count == readingEnergyCount ) {
							long wh = whEnd - whStart;
							if ( wh < maxEnergy ) {
								log.info(
										"OCCP charge session {} on socket {} has only drawn {} Wh since {}; closing session",
										session.getSessionId(), socketId, wh, reading.getTs());
								close = true;
							}
						}
					}
				}
				if ( close ) {
					service.completeChargeSession(session.getIdTag(), session.getSessionId());
				}
			}
		}

	}

}
