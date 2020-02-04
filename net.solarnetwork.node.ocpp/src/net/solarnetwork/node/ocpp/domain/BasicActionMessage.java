/* ==================================================================
 * BasicActionMessage.java - 4/02/2020 5:56:42 pm
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

import ocpp.domain.Action;

/**
 * Basic implementation of {@link ActionMessage}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicActionMessage<T, R> implements ActionMessage<T, R> {

	private final Action action;
	private final T message;
	private final Class<R> resultClass;

	/**
	 * Constructor.
	 * 
	 * @param action
	 *        the action
	 * @param message
	 *        the message
	 * @param resultClass
	 *        the result class
	 */
	public BasicActionMessage(Action action, T message, Class<R> resultClass) {
		super();
		this.action = action;
		this.message = message;
		this.resultClass = resultClass;
	}

	@Override
	public Action getAction() {
		return action;
	}

	@Override
	public T getMessage() {
		return message;
	}

	@Override
	public Class<R> getResultClass() {
		return resultClass;
	}

}
