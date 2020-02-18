/* ==================================================================
 * ChargingScheduleInfo.java - 18/02/2020 3:17:19 pm
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.solarnetwork.domain.Differentiable;

/**
 * Information about a charging schedule.
 * 
 * @author matt
 * @version 1.0
 */
public class ChargingScheduleInfo implements Differentiable<ChargingScheduleInfo> {

	private Duration duration;
	private Instant start;
	private UnitOfMeasure rateUnit;
	private BigDecimal minRate;
	private List<ChargingSchedulePeriodInfo> periods;

	/**
	 * Constructor.
	 * 
	 * @param rateUnit
	 *        the rate unit
	 * @throws IllegalArgumentException
	 *         if {@code rateUnit} is {@literal null}
	 */
	public ChargingScheduleInfo(UnitOfMeasure rateUnit) {
		super();
		setRateUnit(rateUnit);
	}

	/**
	 * Constructor.
	 * 
	 * @param duration
	 *        the schedule duration
	 * @param start
	 *        the schedule start time
	 * @param rateUnit
	 *        the rate unit
	 * @param minRate
	 *        the minimum charge rate
	 * @throws IllegalArgumentException
	 *         if {@code rateUnit} is {@literal null}
	 */
	public ChargingScheduleInfo(Duration duration, Instant start, UnitOfMeasure rateUnit,
			BigDecimal minRate) {
		this(rateUnit);
		this.duration = duration;
		this.start = start;
		this.minRate = minRate;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the info to copy
	 */
	public ChargingScheduleInfo(ChargingScheduleInfo other) {
		this(other.duration, other.start, other.rateUnit, other.minRate);
		if ( other.periods != null ) {
			setPeriods(new ArrayList<>(other.periods));
		}
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 * 
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(ChargingScheduleInfo other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(duration, other.duration)
				&& Objects.equals(start, other.start)
				&& Objects.equals(rateUnit, other.rateUnit)
				&& Objects.equals(minRate, other.minRate)
				&& Objects.equals(periods, other.periods);
		// @formatter:on
	}

	@Override
	public boolean differsFrom(ChargingScheduleInfo other) {
		return !isSameAs(other);
	}

	/**
	 * @return the duration
	 */
	public Duration getDuration() {
		return duration;
	}

	/**
	 * @param duration
	 *        the duration to set
	 */
	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	/**
	 * @return the start
	 */
	public Instant getStart() {
		return start;
	}

	/**
	 * @param start
	 *        the start to set
	 */
	public void setStart(Instant start) {
		this.start = start;
	}

	/**
	 * Get the charging rate unit to use for the configured
	 * {@link #getPeriods()}.
	 * 
	 * @return the unit, never {@literal null}
	 */
	public UnitOfMeasure getRateUnit() {
		return rateUnit;
	}

	/**
	 * Set the charging rate unit to use for the configured
	 * {@link #getPeriods()}.
	 * 
	 * @param rateUnit
	 *        the unit to set
	 * @throws IllegalArgumentException
	 *         if {@code rateUnit} is {@literal null}
	 */
	public void setRateUnit(UnitOfMeasure rateUnit) {
		if ( rateUnit == null ) {
			throw new IllegalArgumentException("The rateUnit parameter must not be null.");
		}
		this.rateUnit = rateUnit;
	}

	/**
	 * @return the minRate
	 */
	public BigDecimal getMinRate() {
		return minRate;
	}

	/**
	 * @param minRate
	 *        the minRate to set
	 */
	public void setMinRate(BigDecimal minRate) {
		this.minRate = minRate;
	}

	/**
	 * Add a period.
	 * 
	 * @param period
	 *        the period to add
	 */
	public void addPeriod(ChargingSchedulePeriodInfo period) {
		List<ChargingSchedulePeriodInfo> list = getPeriods();
		if ( list == null ) {
			list = new ArrayList<>(4);
			setPeriods(list);
		}
		list.add(period);
	}

	/**
	 * Get the periods list.
	 * 
	 * @return the periods
	 */
	public List<ChargingSchedulePeriodInfo> getPeriods() {
		return periods;
	}

	/**
	 * Set the periods list.
	 * 
	 * @param periods
	 *        the periods to set
	 */
	public void setPeriods(List<ChargingSchedulePeriodInfo> periods) {
		this.periods = periods;
	}

}
