/* ==================================================================
 * SampledValue.java - 10/02/2020 9:29:55 am
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

/**
 * A sampled value, e.g. a meter reading.
 * 
 * @author matt
 * @version 1.0
 */
public class SampledValue {

	private final UUID sessionId;
	private final Instant timestamp;
	private final String value;
	private final ReadingContext context;
	private final ValueFormat format;
	private final Measurand measurand;
	private final Phase phase;
	private final Location location;
	private final UnitOfMeasure unit;

	private SampledValue(Builder builder) {
		this.sessionId = builder.sessionId;
		this.timestamp = builder.timestamp;
		this.value = builder.value;
		this.context = builder.context;
		this.format = builder.format;
		this.measurand = builder.measurand;
		this.phase = builder.phase;
		this.location = builder.location;
		this.unit = builder.unit;
	}

	/**
	 * Get the {@link ChargeSession} ID associated with this value.
	 * 
	 * @return the session ID
	 */
	public UUID getSessionId() {
		return sessionId;
	}

	/**
	 * Get the time the sample was captured.
	 * 
	 * @return the timestamp
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * Get the sampled value.
	 * 
	 * <p>
	 * This is treated as a string because it will be a digitally signed value
	 * if {@link #getFormat()} is {@link ValueFormat#SignedData}.
	 * </p>
	 * 
	 * @return the value the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Get the reading context.
	 * 
	 * @return the context
	 */
	public ReadingContext getContext() {
		return context;
	}

	/**
	 * Get the value format.
	 * 
	 * @return the format
	 */
	public ValueFormat getFormat() {
		return format;
	}

	/**
	 * Get the measurement type.
	 * 
	 * @return the measurand
	 */
	public Measurand getMeasurand() {
		return measurand;
	}

	/**
	 * Get the phase.
	 * 
	 * @return the phase
	 */
	public Phase getPhase() {
		return phase;
	}

	/**
	 * Get the location.
	 * 
	 * @return the location
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Get the measurement unit.
	 * 
	 * @return the unit
	 */
	public UnitOfMeasure getUnit() {
		return unit;
	}

	/**
	 * Creates builder to build {@link SampledValue}.
	 * 
	 * @return created builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Get a builder, populated with this instance's values.
	 * 
	 * @return a pre-populated builder
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	/**
	 * Builder to build {@link SampledValue}.
	 */
	public static final class Builder {

		private UUID sessionId;
		private Instant timestamp;
		private String value;
		private ReadingContext context;
		private ValueFormat format;
		private Measurand measurand;
		private Phase phase;
		private Location location;
		private UnitOfMeasure unit;

		private Builder() {
			super();
		}

		private Builder(SampledValue value) {
			super();
			this.sessionId = value.sessionId;
			this.timestamp = value.timestamp;
			this.value = value.value;
			this.context = value.context;
			this.format = value.format;
			this.measurand = value.measurand;
			this.phase = value.phase;
			this.location = value.location;
			this.unit = value.unit;
		}

		public Builder withSessionId(UUID sessionId) {
			this.sessionId = sessionId;
			return this;
		}

		public Builder withTimestamp(Instant timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public Builder withValue(String value) {
			this.value = value;
			return this;
		}

		public Builder withContext(ReadingContext context) {
			this.context = context;
			return this;
		}

		public Builder withFormat(ValueFormat format) {
			this.format = format;
			return this;
		}

		public Builder withMeasurand(Measurand measurand) {
			this.measurand = measurand;
			return this;
		}

		public Builder withPhase(Phase phase) {
			this.phase = phase;
			return this;
		}

		public Builder withLocation(Location location) {
			this.location = location;
			return this;
		}

		public Builder withUnit(UnitOfMeasure unit) {
			this.unit = unit;
			return this;
		}

		public SampledValue build() {
			return new SampledValue(this);
		}
	}

}
