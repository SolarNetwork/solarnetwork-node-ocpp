/* ==================================================================
 * ChargeSession.java - 10/02/2020 8:03:23 am
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

package net.solarnetwork.node.ocpp.domain;

import java.time.Instant;
import java.util.UUID;
import net.solarnetwork.dao.BasicUuidEntity;

/**
 * An entity for tracking an OCPP transaction, which represents a single
 * charging cycle from authorization to end of charging.
 * 
 * @author matt
 * @version 1.0
 */
public class ChargeSession extends BasicUuidEntity {

	private final String authId;
	private final int connectionId;
	private int transactionId;
	private Instant ended;
	private Instant posted;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param created
	 *        the created date
	 */
	public ChargeSession(UUID id, Instant created, String authId, int connectionId) {
		super(id, created);
		this.authId = authId;
		this.connectionId = connectionId;
	}

	/**
	 * Get the {@link Authorization} ID.
	 * 
	 * @return the authorization ID
	 */
	public String getAuthId() {
		return authId;
	}

	/**
	 * Get the Charge Point connection ID.
	 * 
	 * @return the Charge Point connection ID
	 */
	public int getConnectionId() {
		return connectionId;
	}

	/**
	 * Get the assigned transaction ID.
	 * 
	 * @return the transaction ID
	 */
	public int getTransactionId() {
		return transactionId;
	}

	/**
	 * Set the assigned transaction ID.
	 * 
	 * @param transactionId
	 *        the transaction ID to set
	 */
	public void setTransactionId(int transactionId) {
		this.transactionId = transactionId;
	}

	/**
	 * Get the session end date.
	 * 
	 * @return the ended the end date, or {@literal null} if not ended
	 */
	public Instant getEnded() {
		return ended;
	}

	/**
	 * Set the session end date.
	 * 
	 * @param ended
	 *        the end date to set
	 */
	public void setEnded(Instant ended) {
		this.ended = ended;
	}

	/**
	 * Get the session posted date.
	 * 
	 * <p>
	 * The posted date represents a date when this transaction has been posted
	 * to some external service.
	 * </p>
	 * 
	 * @return the posted date or {@literal null} if not posted
	 */
	public Instant getPosted() {
		return posted;
	}

	/**
	 * Set the session posted date.
	 * 
	 * @param posted
	 *        the posted to set
	 */
	public void setPosted(Instant posted) {
		this.posted = posted;
	}

}
