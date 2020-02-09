/* ==================================================================
 * AuthorizationInfo.java - 6/02/2020 7:23:43 pm
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

/**
 * Charge Point authorization information.
 * 
 * @author matt
 * @version 1.0
 */
public class AuthorizationInfo implements Cloneable {

	private final String id;
	private final AuthorizationStatus status;
	private final Instant expiryDate;
	private final String parentIdTag;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID value, e.g. RFID tag ID
	 * @param status
	 *        the associated OCCP status
	 * @param expiryDate
	 *        the expiration date
	 * @param parentIdTag
	 *        a parent ID tag
	 */
	public AuthorizationInfo(String id, AuthorizationStatus status, Instant expiryDate,
			String parentIdTag) {
		super();
		this.id = id;
		this.status = status;
		this.expiryDate = expiryDate;
		this.parentIdTag = parentIdTag;
	}

	private AuthorizationInfo(Builder builder) {
		this(builder.id, builder.status, builder.expiryDate, builder.parentIdTag);
	}

	public String getId() {
		return id;
	}

	public AuthorizationStatus getStatus() {
		return status;
	}

	public Instant getExpiryDate() {
		return expiryDate;
	}

	public String getParentIdTag() {
		return parentIdTag;
	}

	/**
	 * Get a builder, populated with this instance's values.
	 * 
	 * @return a pre-populated builder
	 */
	public Builder toBuilder() {
		// @formatter:off
		return new Builder()
				.withId(id)
				.withStatus(status)
				.withExpiryDate(expiryDate)
				.withParentIdTag(parentIdTag);
		// @formatter:on
	}

	/**
	 * Creates builder to build {@link AuthorizationInfo}.
	 * 
	 * @return created builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder to build {@link AuthorizationInfo}.
	 */
	public static final class Builder {

		private String id;
		private AuthorizationStatus status;
		private Instant expiryDate;
		private String parentIdTag;

		private Builder() {
		}

		public Builder withId(String id) {
			this.id = id;
			return this;
		}

		public Builder withStatus(AuthorizationStatus status) {
			this.status = status;
			return this;
		}

		public Builder withExpiryDate(Instant expiryDate) {
			this.expiryDate = expiryDate;
			return this;
		}

		public Builder withParentIdTag(String parentIdTag) {
			this.parentIdTag = parentIdTag;
			return this;
		}

		public AuthorizationInfo build() {
			return new AuthorizationInfo(this);
		}
	}

}
