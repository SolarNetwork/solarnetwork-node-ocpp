/* ==================================================================
 * OcppControllerServiceTests.java - 14/02/2020 10:58:35 am
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

package net.solarnetwork.node.ocpp.v16.cs.controller.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.ocpp.dao.AuthorizationDao;
import net.solarnetwork.node.ocpp.dao.ChargePointConnectorDao;
import net.solarnetwork.node.ocpp.dao.ChargePointDao;
import net.solarnetwork.node.ocpp.domain.Authorization;
import net.solarnetwork.node.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.node.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.node.ocpp.service.ChargePointRouter;
import net.solarnetwork.node.ocpp.v16.cs.controller.OcppControllerService;
import net.solarnetwork.test.CallingThreadExecutorService;

/**
 * Test cases for the {@link OcppControllerService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppControllerServiceTests {

	private final Executor executor = new CallingThreadExecutorService();
	private ChargePointRouter chargePointRouter;
	private AuthorizationDao authorizationDao;
	private ChargePointDao chargePointDao;
	private ChargePointConnectorDao chargePointConnectorDao;
	private OcppControllerService service;

	@Before
	public void setup() {
		chargePointRouter = EasyMock.createMock(ChargePointRouter.class);
		authorizationDao = EasyMock.createMock(AuthorizationDao.class);
		chargePointDao = EasyMock.createMock(ChargePointDao.class);
		chargePointConnectorDao = EasyMock.createMock(ChargePointConnectorDao.class);

		service = new OcppControllerService(executor, chargePointRouter, authorizationDao,
				chargePointDao, chargePointConnectorDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(chargePointRouter, authorizationDao, chargePointDao, chargePointConnectorDao);
	}

	private void replayAll() {
		EasyMock.replay(chargePointRouter, authorizationDao, chargePointDao, chargePointConnectorDao);
	}

	@Test
	public void auth_ok() {
		// given
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		Authorization auth = new Authorization(idTag, Instant.now());
		auth.setEnabled(true);
		expect(authorizationDao.get(idTag)).andReturn(auth);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize("foobar", idTag);

		// then
		assertThat("Result available", result, notNullValue());
		assertThat("Auth ID", result.getId(), equalTo(auth.getId()));
		assertThat("Auth status", result.getStatus(), equalTo(AuthorizationStatus.Accepted));
		assertThat("Auth expiry", result.getExpiryDate(), equalTo(auth.getExpiryDate()));
		assertThat("Auth parent", result.getParentId(), equalTo(auth.getParentId()));
	}

	@Test
	public void auth_disabled() {
		// given
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		Authorization auth = new Authorization(idTag, Instant.now());
		auth.setEnabled(false);
		expect(authorizationDao.get(idTag)).andReturn(auth);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize("foobar", idTag);

		// then
		assertThat("Result available", result, notNullValue());
		assertThat("Auth ID", result.getId(), equalTo(auth.getId()));
		assertThat("Auth status", result.getStatus(), equalTo(AuthorizationStatus.Blocked));
		assertThat("Auth expiry", result.getExpiryDate(), equalTo(auth.getExpiryDate()));
		assertThat("Auth parent", result.getParentId(), equalTo(auth.getParentId()));
	}

	@Test
	public void auth_expired() {
		// given
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		Authorization auth = new Authorization(idTag, Instant.now());
		auth.setEnabled(true);
		auth.setExpiryDate(Instant.now().minusSeconds(60));
		expect(authorizationDao.get(idTag)).andReturn(auth);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize("foobar", idTag);

		// then
		assertThat("Result available", result, notNullValue());
		assertThat("Auth ID", result.getId(), equalTo(auth.getId()));
		assertThat("Auth status", result.getStatus(), equalTo(AuthorizationStatus.Expired));
		assertThat("Auth expiry", result.getExpiryDate(), equalTo(auth.getExpiryDate()));
		assertThat("Auth parent", result.getParentId(), equalTo(auth.getParentId()));
	}

	@Test
	public void auth_invalid() {
		// given
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		expect(authorizationDao.get(idTag)).andReturn(null);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize("foobar", idTag);

		// then
		assertThat("Result available", result, notNullValue());
		assertThat("Auth ID", result.getId(), equalTo(idTag));
		assertThat("Auth status", result.getStatus(), equalTo(AuthorizationStatus.Invalid));
		assertThat("Auth expiry", result.getExpiryDate(), nullValue());
		assertThat("Auth parent", result.getParentId(), nullValue());
	}

}
