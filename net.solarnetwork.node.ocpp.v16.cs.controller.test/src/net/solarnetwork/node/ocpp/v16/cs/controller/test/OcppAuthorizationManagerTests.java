/* ==================================================================
 * OcppAuthorizationManagerTests.java - 14/02/2020 7:12:01 am
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
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.ocpp.v16.cs.controller.AuthorizationConfig;
import net.solarnetwork.node.ocpp.v16.cs.controller.OcppAuthorizationManager;
import net.solarnetwork.ocpp.dao.AuthorizationDao;
import net.solarnetwork.ocpp.domain.Authorization;

/**
 * Test cases for the {@link AuthorizationManager} class.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppAuthorizationManagerTests {

	private AuthorizationDao chargePointDao;

	@Before
	public void setup() {
		chargePointDao = EasyMock.createMock(AuthorizationDao.class);
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
		OcppAuthorizationManager mgr = new OcppAuthorizationManager(chargePointDao);
		expect(chargePointDao.getAll(anyObject())).andReturn(emptyList());

		// when
		replayAll();
		mgr.setEntitiesCount(1);

		// then
		assertThat("New count", mgr.getEntitiesCount(), equalTo(1));
		assertThat("Conf list size", mgr.getEntities(), hasSize(1));

		AuthorizationConfig conf = mgr.getEntities().get(0);
		assertThat("New config has no ID", conf.getId(), nullValue());
	}

	@Test
	public void removeConfig() {
		// given
		OcppAuthorizationManager mgr = new OcppAuthorizationManager(chargePointDao);

		Authorization cp = new Authorization(1L, Instant.now());
		cp.setExpiryDate(Instant.now().plusSeconds(60));
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
		OcppAuthorizationManager mgr = new OcppAuthorizationManager(chargePointDao);

		Authorization cp = new Authorization(1L, Instant.now());
		cp.setExpiryDate(Instant.now().plusSeconds(60));
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
