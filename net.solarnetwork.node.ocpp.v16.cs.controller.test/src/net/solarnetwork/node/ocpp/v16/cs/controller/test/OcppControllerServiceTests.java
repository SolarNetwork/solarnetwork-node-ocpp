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

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.ocpp.v16.cs.controller.OcppControllerService;
import net.solarnetwork.ocpp.dao.AuthorizationDao;
import net.solarnetwork.ocpp.dao.ChargePointConnectorDao;
import net.solarnetwork.ocpp.dao.ChargePointDao;
import net.solarnetwork.ocpp.domain.Authorization;
import net.solarnetwork.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.service.ChargePointBroker;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import net.solarnetwork.test.CallingThreadExecutorService;
import net.solarnetwork.test.CommonTestUtils;

/**
 * Test cases for the {@link OcppControllerService} class.
 *
 * @author matt
 * @version 1.0
 */
public class OcppControllerServiceTests {

	private final Executor executor = new CallingThreadExecutorService();
	private ChargePointRouter chargePointRouter;
	private ChargePointBroker chargePointBroker;
	private AuthorizationDao authorizationDao;
	private ChargePointDao chargePointDao;
	private ChargePointConnectorDao chargePointConnectorDao;
	private OcppControllerService service;

	@Before
	public void setup() {
		chargePointRouter = EasyMock.createMock(ChargePointRouter.class);
		chargePointBroker = EasyMock.createMock(ChargePointBroker.class);
		authorizationDao = EasyMock.createMock(AuthorizationDao.class);
		chargePointDao = EasyMock.createMock(ChargePointDao.class);
		chargePointConnectorDao = EasyMock.createMock(ChargePointConnectorDao.class);

		service = new OcppControllerService(executor, chargePointRouter, authorizationDao,
				chargePointDao, chargePointConnectorDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(chargePointRouter, chargePointBroker, authorizationDao, chargePointDao,
				chargePointConnectorDao);
	}

	private void replayAll() {
		EasyMock.replay(chargePointRouter, chargePointBroker, authorizationDao, chargePointDao,
				chargePointConnectorDao);
	}

	private ChargePointIdentity createClientId() {
		return createClientId(UUID.randomUUID().toString());
	}

	private ChargePointIdentity createClientId(String identifier) {
		return new ChargePointIdentity(identifier, ChargePointIdentity.ANY_USER);
	}

	@Test
	public void auth_ok() {
		// given
		ChargePointIdentity identity = createClientId();
		Long id = UUID.randomUUID().getMostSignificantBits();
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		Authorization auth = new Authorization(id, Instant.now());
		auth.setToken(idTag);
		auth.setEnabled(true);
		expect(authorizationDao.getForToken(idTag)).andReturn(auth);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize(identity, idTag);

		// then
		assertThat("Result available", result, notNullValue());
		assertThat("Auth ID", result.getId(), equalTo(auth.getToken()));
		assertThat("Auth status", result.getStatus(), equalTo(AuthorizationStatus.Accepted));
		assertThat("Auth expiry", result.getExpiryDate(), equalTo(auth.getExpiryDate()));
		assertThat("Auth parent", result.getParentId(), equalTo(auth.getParentId()));
	}

	@Test
	public void auth_disabled() {
		// given
		ChargePointIdentity identity = createClientId();
		Long id = UUID.randomUUID().getMostSignificantBits();
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		Authorization auth = new Authorization(id, Instant.now());
		auth.setToken(idTag);
		auth.setEnabled(false);
		expect(authorizationDao.getForToken(idTag)).andReturn(auth);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize(identity, idTag);

		// then
		assertThat("Result available", result, notNullValue());
		assertThat("Auth ID", result.getId(), equalTo(auth.getToken()));
		assertThat("Auth status", result.getStatus(), equalTo(AuthorizationStatus.Blocked));
		assertThat("Auth expiry", result.getExpiryDate(), equalTo(auth.getExpiryDate()));
		assertThat("Auth parent", result.getParentId(), equalTo(auth.getParentId()));
	}

	@Test
	public void auth_expired() {
		// given
		ChargePointIdentity identity = createClientId();
		Long id = UUID.randomUUID().getMostSignificantBits();
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		Authorization auth = new Authorization(id, Instant.now());
		auth.setToken(idTag);
		auth.setEnabled(true);
		auth.setExpiryDate(Instant.now().minusSeconds(60));
		expect(authorizationDao.getForToken(idTag)).andReturn(auth);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize(identity, idTag);

		// then
		assertThat("Result available", result, notNullValue());
		assertThat("Auth ID", result.getId(), equalTo(auth.getToken()));
		assertThat("Auth status", result.getStatus(), equalTo(AuthorizationStatus.Expired));
		assertThat("Auth expiry", result.getExpiryDate(), equalTo(auth.getExpiryDate()));
		assertThat("Auth parent", result.getParentId(), equalTo(auth.getParentId()));
	}

	@Test
	public void auth_invalid() {
		// given
		ChargePointIdentity identity = createClientId();
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		expect(authorizationDao.getForToken(idTag)).andReturn(null);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize(identity, idTag);

		// then
		assertThat("Result available", result, notNullValue());
		assertThat("Auth ID", result.getId(), equalTo(idTag));
		assertThat("Auth status", result.getStatus(), equalTo(AuthorizationStatus.Invalid));
		assertThat("Auth expiry", result.getExpiryDate(), nullValue());
		assertThat("Auth parent", result.getParentId(), nullValue());
	}

	@Test
	public void register_new() {
		// GIVEN
		String identifier = UUID.randomUUID().toString();
		ChargePointIdentity identity = createClientId(identifier);

		// look for existing charge point: not found
		expect(chargePointDao.getForIdentity(identity)).andReturn(null);

		// save new charge point
		Capture<ChargePoint> chargePointCaptor = Capture.newInstance(CaptureType.ALL);
		final Long chargePointId = CommonTestUtils.randomLong();
		expect(chargePointDao.save(capture(chargePointCaptor))).andReturn(chargePointId);

		final ChargePoint daoChargePoint = new ChargePoint(chargePointId);
		expect(chargePointDao.get(chargePointId)).andReturn(daoChargePoint);

		// WHEN
		replayAll();
		ChargePointInfo info = new ChargePointInfo();
		info.setId(identifier);
		info.setChargePointVendor("ACME");
		info.setChargePointModel("One");
		ChargePoint result = service.registerChargePoint(identity, info);

		// THEN
		// @formatter:off
		then(result)
			.as("DAO result returned")
			.isSameAs(daoChargePoint)
			;

		then(chargePointCaptor.getValue())
			.as("Inserted charge point identifier preserved")
			.returns(identifier, from(cp -> cp.getInfo().getId()))
			.as("Inserted charge point connectors start at 0")
			.returns(0, from(ChargePoint::getConnectorCount))
			;
		// @formatter:on
	}
}
