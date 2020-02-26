/* ==================================================================
 * JdbcChargePointDaoTests.java - 7/02/2020 12:22:50 pm
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

package net.solarnetwork.node.ocpp.dao.jdbc.test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.ocpp.dao.jdbc.JdbcChargePointDao;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.RegistrationStatus;

/**
 * Test cases for the {@link JdbcChargePointDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargePointDaoTests extends AbstractNodeTransactionalTest {

	@Resource(name = "dataSource")
	private DataSource dataSource;

	private JdbcChargePointDao dao;
	private ChargePoint last;

	@Before
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		dao = new JdbcChargePointDao();
		dao.setDataSource(dataSource);
		dao.init();
	}

	private ChargePoint createTestChargePoint(String vendor, String model) {
		ChargePointInfo info = new ChargePointInfo(UUID.randomUUID().toString());
		info.setChargePointVendor(vendor);
		info.setChargePointModel(model);
		ChargePoint cp = new ChargePoint(null, Instant.ofEpochMilli(System.currentTimeMillis()), info);
		cp.setEnabled(true);
		cp.setRegistrationStatus(RegistrationStatus.Unknown);
		cp.setConnectorCount(2);
		return cp;
	}

	@Test
	public void insert() {
		ChargePoint cp = createTestChargePoint("foo", "bar");
		Long pk = dao.save(cp);
		assertThat("PK generated", pk, notNullValue());
		last = new ChargePoint(pk, cp.getCreated(), cp.getInfo());
		last.setEnabled(cp.isEnabled());
		last.setRegistrationStatus(cp.getRegistrationStatus());
		last.setConnectorCount(cp.getConnectorCount());
	}

	@Test
	public void getByPK() {
		insert();
		ChargePoint entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Connector count", entity.getConnectorCount(), equalTo(last.getConnectorCount()));
	}

	@Test
	public void update() {
		insert();
		ChargePoint cp = dao.get(last.getId());
		cp.setRegistrationStatus(RegistrationStatus.Rejected);
		cp.getInfo().setChargePointVendor("Updated Vendor");
		cp.setConnectorCount(3);
		Long pk = dao.save(cp);
		assertThat("PK unchanged", pk, equalTo(cp.getId()));

		ChargePoint entity = dao.get(pk);
		assertThat("Registration status updated", entity.getRegistrationStatus(),
				equalTo(RegistrationStatus.Rejected));
		assertThat("Vendor updated", entity.getInfo().getChargePointVendor(),
				equalTo(cp.getInfo().getChargePointVendor()));
		assertThat("Connector count updated", entity.getConnectorCount(),
				equalTo(cp.getConnectorCount()));
	}

	@Test
	public void findAll() {
		ChargePoint obj1 = createTestChargePoint("foo", "bar");
		obj1 = dao.get(dao.save(obj1));
		ChargePoint obj2 = new ChargePoint(null, obj1.getCreated().minusSeconds(60),
				new ChargePointInfo("b", "foo", "bar"));
		obj2 = dao.get(dao.save(obj2));
		ChargePoint obj3 = new ChargePoint(null, obj1.getCreated().plusSeconds(60),
				new ChargePointInfo("c", "foo", "bar"));
		obj3 = dao.get(dao.save(obj3));

		Collection<ChargePoint> results = dao.getAll(null);
		assertThat("Results found in order", results, contains(obj2, obj1, obj3));
	}

	@Test
	public void findByIdentifier_none() {
		ChargePoint entity = dao.getForIdentifier(new ChargePointIdentity("foo", "bar"));
		assertThat("No users", entity, nullValue());
	}

	@Test
	public void findByIdentifier_noMatch() {
		insert();
		ChargePoint entity = dao.getForIdentifier(new ChargePointIdentity("not a match", "bar"));
		assertThat("No match", entity, nullValue());
	}

	@Test
	public void findByIdentifier() {
		findAll();
		ChargePoint entity = dao.getForIdentifier(new ChargePointIdentity("b", "c"));
		assertThat("Match", entity, notNullValue());
		assertThat("Identifier matches", entity.getInfo().getId(), equalTo("b"));
	}
}
