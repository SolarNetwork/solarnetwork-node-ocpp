/* ==================================================================
 * JdbcChargeSessionDaoTests.java - 10/02/2020 11:40:37 am
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
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.ocpp.dao.jdbc.JdbcChargePointDao;
import net.solarnetwork.node.ocpp.dao.jdbc.JdbcChargeSessionDao;
import net.solarnetwork.node.ocpp.domain.ChargePoint;
import net.solarnetwork.node.ocpp.domain.ChargePointInfo;
import net.solarnetwork.node.ocpp.domain.ChargeSession;
import net.solarnetwork.node.ocpp.domain.ChargeSessionEndReason;
import net.solarnetwork.node.ocpp.domain.RegistrationStatus;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;

/**
 * Test cases for the {@link JdbcChargeSessionDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargeSessionDaoTests extends AbstractNodeTransactionalTest {

	@Resource(name = "dataSource")
	private DataSource dataSource;

	private JdbcChargePointDao chargePointDao;

	private JdbcChargeSessionDao dao;
	private ChargeSession last;

	@Before
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		chargePointDao = new JdbcChargePointDao();
		chargePointDao.setDataSource(dataSource);
		chargePointDao.init();

		dao = new JdbcChargeSessionDao();
		dao.setDataSource(dataSource);
		dao.init();
	}

	private ChargePoint createTestChargePoint(String vendor, String model) {
		ChargePoint cp = new ChargePoint(UUID.randomUUID().toString(), Instant.now());
		cp.setEnabled(true);
		cp.setRegistrationStatus(RegistrationStatus.Accepted);

		ChargePointInfo info = new ChargePointInfo();
		info.setChargePointVendor(vendor);
		info.setChargePointModel(model);
		cp.setInfo(info);
		cp.setConnectorCount(2);
		return cp;
	}

	private ChargeSession createTestChargeSession(String chargePointId) {
		ChargeSession sess = new ChargeSession(UUID.randomUUID(), Instant.now(),
				UUID.randomUUID().toString().substring(0, 20), chargePointId, 1, 0);
		return sess;
	}

	@Test
	public void insert() {
		ChargePoint cp = createTestChargePoint("foo", "bar");
		chargePointDao.save(cp);

		ChargeSession sess = createTestChargeSession(cp.getId());
		UUID pk = dao.save(sess);
		assertThat("PK preserved", pk, equalTo(sess.getId()));
		last = sess;
	}

	@Test
	public void getByPK() {
		insert();
		ChargeSession entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Auth ID", entity.getAuthId(), equalTo(last.getAuthId()));
		assertThat("Conn ID", entity.getConnectorId(), equalTo(last.getConnectorId()));
		assertThat("Transaction ID generated", entity.getTransactionId(), greaterThan(0));
	}

	@Test
	public void update() {
		insert();
		ChargeSession sess = dao.get(last.getId());
		sess.setEnded(last.getCreated().plus(1, ChronoUnit.HOURS));
		sess.setEndReason(ChargeSessionEndReason.Local);
		sess.setEndAuthId(last.getAuthId());
		sess.setPosted(last.getCreated().plus(2, ChronoUnit.HOURS));
		UUID pk = dao.save(sess);
		assertThat("PK unchanged", pk, equalTo(sess.getId()));

		ChargeSession entity = dao.get(pk);
		assertThat("Ended updated", entity.getEnded(), equalTo(sess.getEnded()));
		assertThat("Posted updated", entity.getPosted(), equalTo(sess.getPosted()));
	}

}
