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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
import net.solarnetwork.node.ocpp.domain.Location;
import net.solarnetwork.node.ocpp.domain.Measurand;
import net.solarnetwork.node.ocpp.domain.ReadingContext;
import net.solarnetwork.node.ocpp.domain.RegistrationStatus;
import net.solarnetwork.node.ocpp.domain.SampledValue;
import net.solarnetwork.node.ocpp.domain.UnitOfMeasure;
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
		ChargePoint cp = new ChargePoint(UUID.randomUUID().toString(),
				Instant.ofEpochMilli(System.currentTimeMillis()));
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
		ChargeSession sess = new ChargeSession(UUID.randomUUID(),
				Instant.ofEpochMilli(System.currentTimeMillis()),
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

	@Test
	public void findIncomplete_tx_none() {
		ChargeSession sess = dao.getIncompleteChargeSessionForTransaction("n/a", 1);
		assertThat("No incomplete session found", sess, nullValue());
	}

	@Test
	public void findIncomplete_tx_noMatchingId() {
		insert();
		ChargeSession sess = dao.getIncompleteChargeSessionForTransaction("n/a", 1);
		assertThat("No incomplete session found", sess, nullValue());
	}

	@Test
	public void findIncomplete_tx_onlyComplete() {
		insert();

		ChargeSession s = dao.get(last.getId());
		s.setEnded(Instant.ofEpochMilli(System.currentTimeMillis()));
		dao.save(s);

		ChargeSession sess = dao.getIncompleteChargeSessionForTransaction(s.getChargePointId(),
				s.getTransactionId());
		assertThat("No incomplete session found", sess, nullValue());
	}

	@Test
	public void findIncomplete_tx() {
		insert();

		ChargeSession s = dao.get(last.getId());

		ChargeSession sess = dao.getIncompleteChargeSessionForTransaction(s.getChargePointId(),
				s.getTransactionId());
		assertThat("Incomplete session found", sess, equalTo(last));
	}

	@Test
	public void findIncomplete_conn() {
		insert();

		ChargeSession sess = dao.getIncompleteChargeSessionForConnector(last.getChargePointId(),
				last.getConnectorId());
		assertThat("Incomplete session found", sess, equalTo(last));
	}

	@Test
	public void findIncomplete_chargePoint_onlyChargePoint() {
		insert();

		// add another for different charge point
		ChargePoint cp2 = createTestChargePoint("vendor 2", "model 2");
		chargePointDao.save(cp2);
		ChargeSession s = dao.get(last.getId());
		ChargeSession two = new ChargeSession(UUID.randomUUID(),
				Instant.ofEpochMilli(System.currentTimeMillis()), s.getAuthId(), cp2.getId(),
				s.getConnectorId() + 1, s.getTransactionId() + 1);
		dao.save(two);

		Collection<ChargeSession> sess = dao
				.getIncompleteChargeSessionForChargePoint(s.getChargePointId());
		assertThat("Incomplete session found", sess, contains(s));
	}

	@Test
	public void findIncomplete_chargePoint_onlyComplete() {
		insert();

		// add another for same charge point, but complete
		ChargeSession s = dao.get(last.getId());
		ChargeSession two = new ChargeSession(UUID.randomUUID(),
				Instant.ofEpochMilli(System.currentTimeMillis()), s.getAuthId(), s.getChargePointId(),
				s.getConnectorId() + 1, s.getTransactionId() + 1);
		two.setEnded(Instant.ofEpochMilli(System.currentTimeMillis()));
		dao.save(two);

		Collection<ChargeSession> sess = dao
				.getIncompleteChargeSessionForChargePoint(s.getChargePointId());
		assertThat("Incomplete session found", sess, contains(s));
	}

	@Test
	public void findIncomplete_chargePoint() {
		insert();

		// add another for same charge point, also incomplete
		ChargeSession s = dao.get(last.getId());
		ChargeSession two = new ChargeSession(UUID.randomUUID(), s.getCreated().plusSeconds(1),
				s.getAuthId(), s.getChargePointId(), s.getConnectorId() + 1, s.getTransactionId() + 1);
		dao.save(two);

		Collection<ChargeSession> sess = dao
				.getIncompleteChargeSessionForChargePoint(s.getChargePointId());
		assertThat("Incomplete session found", sess, contains(s, two));
	}

	private List<SampledValue> createTestReadings() {
		// @formatter:off
		SampledValue v1 = SampledValue.builder().withSessionId(last.getId())
				.withTimestamp(Instant.ofEpochMilli(System.currentTimeMillis()).minusSeconds(60))
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("1234")
				.build();
		SampledValue v2 = SampledValue.builder().withSessionId(last.getId())
				.withTimestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
				.withContext(ReadingContext.TransactionEnd)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("4321")
				.build();
		// @formatter:on
		return Arrays.asList(v1, v2);
	}

	@Test
	public void addReadings() {
		insert();
		dao.addReadings(createTestReadings());
	}

	@Test
	public void findReadings() {
		insert();
		List<SampledValue> expected = createTestReadings();
		dao.addReadings(expected);

		List<SampledValue> results = dao.findReadingsForSession(last.getId());
		assertThat("Readings found", results, equalTo(expected));
	}

	@Test
	public void findReadings_none() {
		insert();

		List<SampledValue> results = dao.findReadingsForSession(last.getId());
		assertThat("Readings found", results, hasSize(0));
	}

	@Test
	public void deletePosted() {
		insert();

		ChargeSession s = dao.get(last.getId());
		s.setPosted(Instant.ofEpochMilli(System.currentTimeMillis()));
		dao.save(s);

		int result = dao.deletePostedChargeSessions(s.getPosted().plusSeconds(1));
		assertThat("Deleted posted", result, equalTo(1));
		assertThat("Table empty", jdbcTemplate.queryForObject(
				"select count(*) from solarnode.ocpp_charge_sess", Integer.class), equalTo(0));
	}

	@Test
	public void deletePosted_onlyOlder() {
		insert();

		ChargeSession s = dao.get(last.getId());
		s.setPosted(Instant.ofEpochMilli(System.currentTimeMillis()));
		dao.save(s);

		ChargeSession two = new ChargeSession(UUID.randomUUID(), s.getCreated(), s.getAuthId(),
				s.getChargePointId(), s.getConnectorId() + 1, s.getTransactionId() + 1);
		two.setPosted(s.getPosted().minusSeconds(1));
		dao.save(two);

		int result = dao.deletePostedChargeSessions(s.getPosted());
		assertThat("Deleted posted", result, equalTo(1));
		assertThat("Remaining sessions", dao.getAll(null), contains(s));
	}

	@Test
	public void deletePosted_noIncomplete() {
		insert();

		ChargeSession s = dao.get(last.getId());
		dao.save(s);

		ChargeSession two = new ChargeSession(UUID.randomUUID(), s.getCreated(), s.getAuthId(),
				s.getChargePointId(), s.getConnectorId() + 1, s.getTransactionId() + 1);
		dao.save(two);

		int result = dao.deletePostedChargeSessions(
				Instant.ofEpochMilli(System.currentTimeMillis()).plusSeconds(1));
		assertThat("Deleted posted", result, equalTo(0));
		assertThat("Remaining sessions", dao.getAll(null), contains(s, two));
	}
}
