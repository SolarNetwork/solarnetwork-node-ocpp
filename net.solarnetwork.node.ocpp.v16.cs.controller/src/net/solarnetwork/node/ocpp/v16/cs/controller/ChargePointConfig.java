/* ==================================================================
 * ChargePointConfig.java - 13/02/2020 11:46:11 am
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.RegistrationStatus;

/**
 * Configuration object for {@link ChargePoint} entities.
 * 
 * @author matt
 * @version 1.0
 */
public class ChargePointConfig implements Identity<String> {

	/**
	 * A default property value used for required properties like
	 * {@code chargePointVendor} and {@code chargePointModel}.
	 */
	public static final String DEFAULT_PROPERTY_VALUE = "N/A";

	private String id;
	private Instant created;
	private boolean enabled;
	private RegistrationStatus registrationStatus;
	private ChargePointInfo info;

	/**
	 * Constructor.
	 */
	public ChargePointConfig() {
		super();
		setEnabled(true);
		ChargePointInfo info = new ChargePointInfo();
		info.setChargePointVendor(DEFAULT_PROPERTY_VALUE);
		info.setChargePointModel(DEFAULT_PROPERTY_VALUE);
		setInfo(info);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param chargePoint
	 *        the Charge Point to copy properties from
	 */
	public ChargePointConfig(ChargePoint chargePoint) {
		super();
		if ( chargePoint == null ) {
			return;
		}
		setId(chargePoint.getId());
		setCreated(chargePoint.getCreated());
		setEnabled(chargePoint.isEnabled());
		setRegistrationStatus(chargePoint.getRegistrationStatus());
		setInfo(chargePoint.getInfo());
	}

	@Override
	public int compareTo(String o) {
		return id.compareTo(o);
	}

	/**
	 * Get settings for this config.
	 * 
	 * @param messageSource
	 *        the message source
	 * @param locale
	 *        the desired locale
	 * @param prefix
	 *        a prefix
	 * @return the settings
	 */
	public List<SettingSpecifier> settings(MessageSource messageSource, Locale locale, String prefix) {
		if ( prefix == null ) {
			prefix = "";
		}
		List<SettingSpecifier> results = new ArrayList<>(5);
		results.add(new BasicTitleSettingSpecifier(prefix + "info", info(messageSource, locale), true));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "id", getId()));
		results.add(new BasicToggleSettingSpecifier(prefix + "enabled", isEnabled()));

		// drop-down menu for function
		BasicMultiValueSettingSpecifier statusSpec = new BasicMultiValueSettingSpecifier(
				prefix + "registrationStatusCode", String.valueOf(getRegistrationStatusCode()));
		Map<String, String> statusTitles = new LinkedHashMap<String, String>(4);
		for ( RegistrationStatus e : RegistrationStatus.values() ) {
			statusTitles.put(String.valueOf(e.codeValue()), e.toString());
		}
		statusSpec.setValueTitles(statusTitles);
		results.add(statusSpec);

		return results;
	}

	private String info(MessageSource messageSource, Locale locale) {
		if ( id == null || id.isEmpty() ) {
			return "N/A";
		}
		// TODO: better info, and i18n
		StringBuilder buf = new StringBuilder();
		if ( messageSource != null ) {
			buf.append(
					messageSource.getMessage("info.registeredOn.title", null, "Registered on:", locale));
		} else {
			buf.append("Registered on:");
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
				.withLocale(locale).withZone(ZoneId.systemDefault());
		buf.append(" ").append(formatter.format(created));

		if ( info != null ) {
			if ( info.getChargePointVendor() != null ) {
				buf.append("; ").append(info.getChargePointVendor());
				if ( info.getChargePointModel() != null ) {
					buf.append(" ").append(info.getChargePointModel());
					if ( info.getFirmwareVersion() != null ) {
						buf.append(" ").append(info.getFirmwareVersion());
					}
				}
			}
			if ( info.getChargePointSerialNumber() != null ) {
				buf.append("; ").append(info.getChargePointSerialNumber());
			}
		}
		return buf.toString();
	}

	/**
	 * Get the ID.
	 * 
	 * @return the Charge Point ID
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * Set the ID.
	 * 
	 * @param id
	 *        the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get the creation date.
	 * 
	 * @return the created
	 */
	public Instant getCreated() {
		return created;
	}

	/**
	 * Set the creation date.
	 * 
	 * @param created
	 *        the created to set
	 */
	public void setCreated(Instant created) {
		this.created = created;
	}

	/**
	 * Get the enabled flag.
	 * 
	 * @return the enabled flag
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the enabled flag.
	 * 
	 * @param enabled
	 *        the enabled flag to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Get the registration status.
	 * 
	 * @return the registrationStatus
	 */
	public RegistrationStatus getRegistrationStatus() {
		return registrationStatus;
	}

	/**
	 * Set the registration status.
	 * 
	 * @param registrationStatus
	 *        the registrationStatus to set
	 */
	public void setRegistrationStatus(RegistrationStatus registrationStatus) {
		this.registrationStatus = registrationStatus;
	}

	/**
	 * Get the registration status code value.
	 * 
	 * @return the registration status code
	 */
	public int getRegistrationStatusCode() {
		RegistrationStatus s = getRegistrationStatus();
		return (s != null ? s.codeValue() : RegistrationStatus.Unknown.codeValue());
	}

	/**
	 * Set the registration status as a code value.
	 * 
	 * @param registrationStatus
	 *        the registrationStatus to set
	 */
	public void setRegistrationStatusCode(int code) {
		setRegistrationStatus(RegistrationStatus.forCode(code));
	}

	/**
	 * Get the info.
	 * 
	 * @return the info
	 */
	public ChargePointInfo getInfo() {
		return info;
	}

	/**
	 * Set the info.
	 * 
	 * @param info
	 *        the info to set
	 */
	public void setInfo(ChargePointInfo info) {
		this.info = info;
	}

}
