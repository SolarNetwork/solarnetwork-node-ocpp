/* ==================================================================
 * ChargingProfileConfig.java - 19/02/2020 1:32:39 pm
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.node.ocpp.domain.ChargingProfile;
import net.solarnetwork.node.ocpp.domain.ChargingProfileInfo;
import net.solarnetwork.node.ocpp.domain.ChargingProfileKind;
import net.solarnetwork.node.ocpp.domain.ChargingProfilePurpose;
import net.solarnetwork.node.ocpp.domain.ChargingScheduleInfo;
import net.solarnetwork.node.ocpp.domain.ChargingScheduleRecurrency;
import net.solarnetwork.node.ocpp.domain.UnitOfMeasure;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;

/**
 * Configuration for an
 * {@link net.solarnetwork.node.ocpp.domain.ChargingProfile}.
 * 
 * @author matt
 * @version 1.0
 */
public class ChargingProfileConfig implements Identity<UUID> {

	private UUID id;
	private ChargingProfileInfo info;

	/**
	 * Constructor.
	 */
	public ChargingProfileConfig() {
		super();
		id = UUID.randomUUID();
		info = new ChargingProfileInfo(ChargingProfilePurpose.Unknown, ChargingProfileKind.Unknown,
				new ChargingScheduleInfo(UnitOfMeasure.Unknown));
	}

	/**
	 * Copy constructor.
	 * 
	 * @param auth
	 *        the authorization entity to copy values from
	 */
	public ChargingProfileConfig(ChargingProfile profile) {
		super();
		if ( profile == null ) {
			return;
		}
		this.id = profile.getId();
		setInfo(new ChargingProfileInfo(profile.getInfo()));
	}

	@Override
	public int compareTo(UUID o) {
		return id.compareTo(o);
	}

	/**
	 * Get the setting specifiers for a {@link ChargingProfileConfig}.
	 * 
	 * @param prefix
	 *        the prefix to use for each setting key
	 * @return the settings
	 */
	public List<SettingSpecifier> settings(String prefix, MessageSource messageSource, Locale locale) {
		if ( prefix == null ) {
			prefix = "";
		}
		List<SettingSpecifier> results = new ArrayList<>(4);
		results.add(new BasicTitleSettingSpecifier(prefix + "id", id.toString(), true));

		// drop-down menu for purpose
		BasicMultiValueSettingSpecifier purposeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "info.purposeCode", String.valueOf(info.getPurposeCode()));
		Map<String, String> purposeTitles = new LinkedHashMap<String, String>(2);
		for ( ChargingProfilePurpose e : ChargingProfilePurpose.values() ) {
			if ( e == ChargingProfilePurpose.TxProfile ) {
				continue;
			}
			purposeTitles.put(String.valueOf(e.codeValue()), messageSource
					.getMessage("ChargingProfilePurpose." + e.name(), null, e.toString(), locale));
		}
		purposeSpec.setValueTitles(purposeTitles);
		results.add(purposeSpec);

		// drop-down menu for kind
		BasicMultiValueSettingSpecifier kindSpec = new BasicMultiValueSettingSpecifier(
				prefix + "info.kindCode", String.valueOf(info.getKindCode()));
		Map<String, String> kindTitles = new LinkedHashMap<String, String>(2);
		for ( ChargingProfileKind e : ChargingProfileKind.values() ) {
			kindTitles.put(String.valueOf(e.codeValue()), messageSource
					.getMessage("ChargingProfileKind." + e.name(), null, e.toString(), locale));
		}
		kindSpec.setValueTitles(kindTitles);
		results.add(kindSpec);

		// drop-down menu for recurrency
		BasicMultiValueSettingSpecifier recurSpec = new BasicMultiValueSettingSpecifier(
				prefix + "info.recurrencyCode", String.valueOf(info.getRecurrencyCode()));
		Map<String, String> recurTitles = new LinkedHashMap<String, String>(2);
		for ( ChargingScheduleRecurrency e : ChargingScheduleRecurrency.values() ) {
			recurTitles.put(String.valueOf(e.codeValue()), messageSource
					.getMessage("ChargingScheduleRecurrency." + e.name(), null, e.toString(), locale));
		}
		recurSpec.setValueTitles(recurTitles);
		results.add(recurSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "info.validFromValue",
				info.getValidFromValue()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "info.validToValue",
				info.getValidToValue()));

		// TODO
		return results;
	}

	/**
	 * Get the ID.
	 * 
	 * @return the id
	 */
	@Override
	public UUID getId() {
		return id;
	}

	/**
	 * @return the info
	 */
	public ChargingProfileInfo getInfo() {
		return info;
	}

	/**
	 * @param info
	 *        the info to set
	 */
	public void setInfo(ChargingProfileInfo info) {
		this.info = info;
	}

}
