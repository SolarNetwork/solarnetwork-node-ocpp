/* ==================================================================
 * OcppRegistrationManager.java - 13/02/2020 11:25:38 am
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

package net.solarnetwork.node.ocpp.v16.cs.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.ocpp.dao.ChargePointDao;
import net.solarnetwork.node.ocpp.domain.ChargePoint;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.settings.SettingsChangeObserver;

/**
 * Manager for Charge Point registrations.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppRegistrationManager implements SettingSpecifierProvider, SettingsChangeObserver {

	private final ChargePointDao chargePointDao;
	private MessageSource messageSource;
	private List<ChargePointConfig> chargePoints;
	private int chargePointsCount;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param chargePointDao
	 *        the DAO to manage charge points with
	 */
	public OcppRegistrationManager(ChargePointDao chargePointDao) {
		super();
		this.chargePointDao = chargePointDao;
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		if ( properties == null || properties.isEmpty() ) {
			return;
		}
		Map<String, ChargePoint> cpMap = chargePointDao.getAll(null).stream()
				.collect(Collectors.toMap(ChargePoint::getId, a -> a));
		List<ChargePointConfig> configs = chargePoints;
		Iterator<ChargePointConfig> confItr = (configs != null ? configs.iterator()
				: Collections.emptyIterator());
		while ( confItr.hasNext() ) {
			ChargePointConfig conf = confItr.next();
			if ( conf.getId() == null || conf.getId().isEmpty() ) {
				continue;
			}
			ChargePoint chargePoint = cpMap.remove(conf.getId());
			if ( chargePoint == null ) {
				chargePoint = new ChargePoint(conf.getId(), Instant.now());
			}
			ChargePoint orig = new ChargePoint(chargePoint);
			chargePoint.setEnabled(conf.isEnabled());
			chargePoint.setRegistrationStatus(conf.getRegistrationStatus());
			if ( !chargePoint.isSameAs(orig) ) {
				log.info("Saving OCPP authorization: {}", chargePoint);
				chargePointDao.save(chargePoint);
			}
		}
		for ( ChargePoint old : cpMap.values() ) {
			chargePointDao.delete(old);
		}
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.v16.cs.controller.OcppRegistrationManager";
	}

	@Override
	public String getDisplayName() {
		return "OCPP ChargePoint Manager";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	private synchronized List<ChargePointConfig> loadChargePoints() {
		Collection<ChargePoint> result = chargePointDao.getAll(null);
		List<ChargePointConfig> configs = (result != null
				? result.stream().map(ChargePointConfig::new).collect(Collectors.toList())
				: new ArrayList<>());
		this.chargePoints = configs;
		while ( configs.size() < this.chargePointsCount ) {
			chargePoints.add(new ChargePointConfig());
		}
		return configs;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(1);

		List<ChargePointConfig> configs = loadChargePoints();
		results.add(SettingsUtil.dynamicListSettingSpecifier("chargePoints", configs,
				new SettingsUtil.KeyedListCallback<ChargePointConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(ChargePointConfig value,
							int index, String key) {
						return Collections.singletonList(new BasicGroupSettingSpecifier(
								value.settings(messageSource, Locale.getDefault(), key + ".")));
					}
				}));

		return results;
	}

	/**
	 * Set the message source.
	 * 
	 * @param messageSource
	 *        the message source to set
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Get the list of charge points.
	 * 
	 * @return the authorizations
	 */
	public synchronized List<ChargePointConfig> getChargePoints() {
		if ( chargePoints == null ) {
			loadChargePoints();
		}
		return chargePoints;
	}

	/**
	 * Set the list of charge points.
	 * 
	 * @param chargePoints
	 *        the charge points to set
	 */
	public synchronized void setChargePoints(List<ChargePointConfig> chargePoints) {
		this.chargePoints = chargePoints;
	}

	/**
	 * Get the count of charge points.
	 * 
	 * @return the charge point count
	 */
	public int getChargePointsCount() {
		return chargePointsCount;
	}

	/**
	 * Adjust the number of configured {@link ChargePointConfig} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link ChargePointConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        the desired number of elements
	 */
	public void setChargePointsCount(int count) {
		this.chargePointsCount = count;
		List<ChargePointConfig> chargePoints = getChargePoints();
		int currCount = (chargePoints != null ? chargePoints.size() : 0);
		if ( currCount == count ) {
			return;
		}
		while ( currCount < count ) {
			chargePoints.add(new ChargePointConfig());
			currCount++;
		}
		while ( currCount > count ) {
			chargePoints.remove(--currCount);
		}
	}
}
