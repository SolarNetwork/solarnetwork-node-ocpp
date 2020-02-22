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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.ocpp.dao.jdbc.JdbcChargePointDao;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.ocpp.domain.ChargePoint;
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
		ChargePoint cp = new ChargePoint(UUID.randomUUID().toString(),
				Instant.ofEpochMilli(System.currentTimeMillis()));
		cp.setEnabled(true);
		cp.setRegistrationStatus(RegistrationStatus.Unknown);

		ChargePointInfo info = new ChargePointInfo();
		info.setChargePointVendor(vendor);
		info.setChargePointModel(model);
		cp.setInfo(info);
		cp.setConnectorCount(2);
		return cp;
	}

	@Test
	public void insert() {
		ChargePoint cp = createTestChargePoint("foo", "bar");
		String pk = dao.save(cp);
		assertThat("PK preserved", pk, equalTo(cp.getId()));
		last = cp;
	}

	@Test
	public void getByPK() {
		insert();
		ChargePoint entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Connector count", entity.getConnectorCount(), equalTo(last.getConnectorCount()));
		// TODO
	}

	@Test
	public void update() {
		insert();
		ChargePoint cp = dao.get(last.getId());
		cp.setRegistrationStatus(RegistrationStatus.Rejected);
		cp.getInfo().setChargePointVendor("Updated Vendor");
		cp.setConnectorCount(3);
		String pk = dao.save(cp);
		assertThat("PK unchanged", pk, equalTo(cp.getId()));

		ChargePoint entity = dao.get(pk);
		assertThat("Registration status updated", entity.getRegistrationStatus(),
				equalTo(RegistrationStatus.Rejected));
		assertThat("Vendor updated", entity.getInfo().getChargePointVendor(),
				equalTo(cp.getInfo().getChargePointVendor()));
		assertThat("Connector count updated", entity.getConnectorCount(),
				equalTo(cp.getConnectorCount()));
	}

}
