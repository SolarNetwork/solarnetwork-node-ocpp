/* ==================================================================
 * CentralSystemServiceFactorySupport.java - 9/06/2015 11:05:18 am
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

package net.solarnetwork.node.ocpp.v15.cp.support;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.node.ocpp.v15.cp.CentralSystemServiceFactory;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.Identifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * A base helper class for services that require use of
 * {@link CentralSystemServiceFactory}.
 * 
 * <p>
 * The {@link FilterableService} API can be used to allow dynamic runtime
 * resolution of which central service to use, if more than one are deployed.
 * </p>
 * 
 * <p>
 * This class also implements {@link Identifiable} and will delegate those
 * methods to the configured {@link CentralSystemServiceFactory} if not
 * explicitly defined on this class.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public abstract class CentralSystemServiceFactorySupport extends BaseIdentifiable
		implements SettingSpecifierProvider, Identifiable {

	private CentralSystemServiceFactory centralSystem;

	private final DatatypeFactory datatypeFactory;
	private final GregorianCalendar utcCalendar;

	/**
	 * Default constructor.
	 */
	public CentralSystemServiceFactorySupport() {
		super();
		utcCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch ( DatatypeConfigurationException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get a {@link DatatypeFactory} instance.
	 * 
	 * @return The factory.
	 */
	protected final DatatypeFactory getDatatypeFactory() {
		return datatypeFactory;
	}

	/**
	 * Get a {@link XMLGregorianCalendar} for the current time, set to the UTC
	 * time zone.
	 * 
	 * @return A new calendar instance.
	 */
	protected final XMLGregorianCalendar newXmlCalendar() {
		return newXmlCalendar(System.currentTimeMillis());
	}

	/**
	 * Get a {@link XMLGregorianCalendar} for a specific time, set to the UTC
	 * time zone.
	 * 
	 * @param date
	 *        The date, in milliseconds since the epoch.
	 * @return A new calendar instance.
	 */
	protected final XMLGregorianCalendar newXmlCalendar(long date) {
		GregorianCalendar now = (GregorianCalendar) utcCalendar.clone();
		now.setTimeInMillis(date);
		return datatypeFactory.newXMLGregorianCalendar(now);
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(3);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(Locale.getDefault()), true));
		results.add(new BasicTextFieldSettingSpecifier("filterableCentralSystem.propertyFilters['UID']",
				"OCPP Central System"));
		return results;
	}

	/**
	 * Get a status message to display as a read-only setting.
	 * 
	 * @param locale
	 *        The desired locale of the message.
	 * @return The status message.
	 */
	protected abstract String getInfoMessage(Locale locale);

	/**
	 * Get the {@link CentralSystemServiceFactory} to use.
	 * 
	 * @return The configured central system.
	 */
	public final CentralSystemServiceFactory getCentralSystem() {
		return centralSystem;
	}

	/**
	 * Set the {@link CentralSystemServiceFactory} to use. If the provided
	 * object also implements {@link FilterableService} then it will be passed
	 * to {@link #setFilterableCentralSystem(FilterableService)} as well.
	 * 
	 * @param centralSystem
	 *        The central system to use.
	 */
	public final void setCentralSystem(CentralSystemServiceFactory centralSystem) {
		this.centralSystem = centralSystem;
	}

	/**
	 * Get the central system to use, as a {@link FilterableService}. If the
	 * configured central system does not also implement
	 * {@link FilterableService} this method returns <em>null</em>. This method
	 * is designed to assist with dynamic runtime configuration, where more than
	 * one {@link CentralSystemServiceFactory} may be available and a filter is
	 * needed to choose the appropriate one to use.
	 * 
	 * @return The central system, as a {@link FilterableService}.
	 */
	public final FilterableService getFilterableCentralSystem() {
		CentralSystemServiceFactory central = centralSystem;
		if ( central instanceof FilterableService ) {
			return (FilterableService) central;
		}
		return null;
	}

	/**
	 * Returns the {@code uid} value if configured, or else falls back to
	 * returning {@link CentralSystemServiceFactory#getUID()}.
	 */
	@Override
	public final String getUid() {
		String id = super.getUid();
		if ( id == null ) {
			CentralSystemServiceFactory system = centralSystem;
			if ( system != null ) {
				try {
					id = system.getUid();
				} catch ( RuntimeException e ) {
					log.debug("Error getting central system UID: {}", e.getMessage());
				}
			}
		}
		return id;
	}

	/**
	 * Returns the {@code groupUID} value if configured, or else falls back to
	 * returning {@link CentralSystemServiceFactory#getGroupUID()}.
	 */
	@Override
	public final String getGroupUid() {
		String id = super.getGroupUid();
		if ( id == null ) {
			CentralSystemServiceFactory system = centralSystem;
			if ( system != null ) {
				try {
					id = system.getGroupUid();
				} catch ( RuntimeException e ) {
					log.debug("Error getting central system Group UID: {}", e.getMessage());
				}
			}
		}
		return id;
	}

	/**
	 * Manage a scheduled job based on a repeating interval.
	 * 
	 * This method will return the created trigger, which should be passed on
	 * subsequent calls if the interval is to be changed or unscheduled.
	 * 
	 * @param scheduler
	 *        The scheduler to use.
	 * @param interval
	 *        The interval, in seconds, for the job to be scheduled at. Pass
	 *        {@code 0} or less to unschedule the job.
	 * @param currTrigger
	 *        The current job trigger, or {@code null} if not scheduled.
	 * @param job
	 *        the job to run.
	 * @param jobDescription
	 *        A description to use for the job. This value is included in log
	 *        messages.
	 * @return The scheduled job trigger, or {@code null} if an error occurs or
	 *         the job is unscheduled.
	 * @since 2.0
	 */
	protected ScheduledFuture<?> scheduleIntervalJob(final TaskScheduler scheduler, final int interval,
			final ScheduledFuture<?> currTrigger, final Runnable job, final String jobDescription) {
		if ( scheduler == null ) {
			log.warn("No scheduler avaialable, cannot schedule {} job", jobDescription);
			return null;
		}
		if ( currTrigger != null && !currTrigger.isDone() ) {
			currTrigger.cancel(true);
		}

		if ( interval < 1 ) {
			return null;
		}

		return scheduler.scheduleWithFixedDelay(job, interval * 1000L);
	}

}
