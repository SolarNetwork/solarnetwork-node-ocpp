/* ==================================================================
 * ChargingProfileStatus.java - 18/02/2020 4:10:30 pm
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

/**
 * Enumeration of charging profile status values.
 * 
 * @author matt
 * @version 1.0
 */
public enum ChargingProfileStatus {

	Unknown(0),

	Accepted(1),

	Rejected(2),

	NotSupported(3);

	private final byte code;

	private ChargingProfileStatus(int code) {
		this.code = (byte) code;
	}

	/**
	 * Get the code value.
	 * 
	 * @return the code value
	 */
	public int codeValue() {
		return code & 0xFF;
	}

	/**
	 * Get an enumeration value for a code value.
	 * 
	 * @param code
	 *        the code
	 * @return the status, never {@literal null} and set to {@link #Unknown} if
	 *         not any other valid code
	 */
	public static ChargingProfileStatus forCode(int code) {
		switch (code) {
			case 1:
				return Accepted;

			case 2:
				return Rejected;

			case 3:
				return NotSupported;

			default:
				return Unknown;
		}
	}

}
