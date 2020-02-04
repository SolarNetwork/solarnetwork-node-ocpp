/* ==================================================================
 * ActionException.java - 4/02/2020 5:46:17 pm
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

import ocpp.domain.ErrorCode;

/**
 * An exception related to an action.
 * 
 * @author matt
 * @version 1.0
 */
public class ErrorCodeException extends RuntimeException {

	private static final long serialVersionUID = -1101790444110593504L;

	private final ErrorCode errorCode;

	/**
	 * Constructor.
	 * 
	 * @param errorCode
	 *        the error code; must not be {@literal null}
	 */
	public ErrorCodeException(ErrorCode errorCode) {
		super();
		this.errorCode = errorCode;
	}

	/**
	 * Constructor.
	 * 
	 * @param errorCode
	 *        the error code; must not be {@literal null}
	 * @param message
	 *        a message
	 */
	public ErrorCodeException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	/**
	 * Constructor.
	 * 
	 * @param errorCode
	 *        the error code; must not be {@literal null}
	 * @param cause
	 *        the cause
	 */
	public ErrorCodeException(ErrorCode errorCode, Throwable cause) {
		super(cause);
		this.errorCode = errorCode;
	}

	/**
	 * Constructor.
	 * 
	 * @param errorCode
	 *        the error code; must not be {@literal null}
	 * @param message
	 *        a message
	 * @param cause
	 *        the cause
	 */
	public ErrorCodeException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	/**
	 * Get the error code.
	 * 
	 * @return the error code
	 */
	public ErrorCode getErrorCode() {
		return errorCode;
	}

}
