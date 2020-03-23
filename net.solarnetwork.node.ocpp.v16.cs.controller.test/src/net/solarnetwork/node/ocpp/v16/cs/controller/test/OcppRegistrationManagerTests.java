/* ==================================================================
 * OcppRegistrationManagerTests.java - 13/02/2020 2:58:35 pm
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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.ocpp.v16.cs.controller.ChargePointConfig;
import net.solarnetwork.node.ocpp.v16.cs.controller.OcppRegistrationManager;
import net.solarnetwork.ocpp.dao.ChargePointDao;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointInfo;

/**
 * Test cases for the {@link OcppRegistrationManager} class.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppRegistrationManagerTests {

	private ChargePointDao chargePointDao;

	@Before
	public void setup() {
		chargePointDao = EasyMock.createMock(ChargePointDao.class);
	}

	@After
	public void teardown() {
		EasyMock.verify(chargePointDao);
	}

	public void replayAll() {
		EasyMock.replay(chargePointDao);
	}

	@Test
	public void addConfig() {
		// given
		OcppRegistrationManager mgr = new OcppRegistrationManager(chargePointDao);
		expect(chargePointDao.getAll(anyObject())).andReturn(emptyList());

		// when
		replayAll();
		mgr.setEntitiesCount(1);

		// then
		assertThat("New count", mgr.getEntitiesCount(), equalTo(1));
		assertThat("Conf list size", mgr.getEntities(), hasSize(1));

		ChargePointConfig conf = mgr.getEntities().get(0);
		assertThat("New config has no ID", conf.getId(), nullValue());
		assertThat("Vendor defaulted", conf.getInfo().getChargePointVendor(),
				equalTo(ChargePointConfig.DEFAULT_PROPERTY_VALUE));
		assertThat("Model defaulted", conf.getInfo().getChargePointModel(),
				equalTo(ChargePointConfig.DEFAULT_PROPERTY_VALUE));
	}

	@Test
	public void removeConfig() {
		// given
		OcppRegistrationManager mgr = new OcppRegistrationManager(chargePointDao);

		ChargePoint cp = new ChargePoint(UUID.randomUUID().getMostSignificantBits(), Instant.now(),
				new ChargePointInfo(UUID.randomUUID().toString()));
		cp.getInfo().setChargePointVendor("Vendor");
		cp.getInfo().setChargePointModel("Model");
		cp.getInfo().setChargePointSerialNumber("ABC123");
		expect(chargePointDao.getAll(anyObject())).andReturn(singletonList(cp));

		// when
		replayAll();
		assertThat("Initial count", mgr.getEntitiesCount(), equalTo(1));
		mgr.setEntitiesCount(0);

		// then
		assertThat("New count", mgr.getEntitiesCount(), equalTo(0));
		assertThat("Conf list size", mgr.getEntities(), hasSize(0));
	}

	@Test
	public void removeConfigAndPersist() {
		// given
		OcppRegistrationManager mgr = new OcppRegistrationManager(chargePointDao);

		ChargePoint cp = new ChargePoint(UUID.randomUUID().getMostSignificantBits(), Instant.now(),
				new ChargePointInfo(UUID.randomUUID().toString()));
		cp.getInfo().setChargePointVendor("Vendor");
		cp.getInfo().setChargePointModel("Model");
		cp.getInfo().setChargePointSerialNumber("ABC123");
		expect(chargePointDao.getAll(anyObject())).andReturn(singletonList(cp)).times(2);

		chargePointDao.delete(cp);

		// when
		replayAll();
		assertThat("Initial count", mgr.getEntitiesCount(), equalTo(1));
		mgr.setEntitiesCount(0);
		mgr.configurationChanged(singletonMap("chargePointsCount", "0"));

		// then
		assertThat("New count", mgr.getEntitiesCount(), equalTo(0));
		assertThat("Conf list size", mgr.getEntities(), hasSize(0));
	}
}
