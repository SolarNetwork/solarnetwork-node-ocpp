/* ==================================================================
 * JdbcChargePointConnectorDaoTests.java - 12/02/2020 5:41:22 pm
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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.ocpp.dao.jdbc.JdbcChargePointConnectorDao;
import net.solarnetwork.node.ocpp.dao.jdbc.JdbcChargePointDao;
import net.solarnetwork.node.ocpp.domain.ChargePoint;
import net.solarnetwork.node.ocpp.domain.ChargePointConnector;
import net.solarnetwork.node.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.node.ocpp.domain.ChargePointErrorCode;
import net.solarnetwork.node.ocpp.domain.ChargePointInfo;
import net.solarnetwork.node.ocpp.domain.ChargePointStatus;
import net.solarnetwork.node.ocpp.domain.RegistrationStatus;
import net.solarnetwork.node.ocpp.domain.StatusNotification;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link JdbcChargePointConnectorDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargePointConnectorDaoTests extends AbstractNodeTransactionalTest {

	@Resource(name = "dataSource")
	private DataSource dataSource;

	private JdbcChargePointDao chargePointDao;

	private JdbcChargePointConnectorDao dao;
	private ChargePointConnector last;

	@Before
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		chargePointDao = new JdbcChargePointDao();
		chargePointDao.setDataSource(dataSource);
		chargePointDao.init();

		dao = new JdbcChargePointConnectorDao();
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

	@Test
	public void insert() {
		ChargePoint cp = createTestChargePoint("foo", "bar");
		chargePointDao.save(cp);

		ChargePointConnector cpc = new ChargePointConnector(new ChargePointConnectorKey(cp.getId(), 1),
				Instant.now());
		// @formatter:off
		cpc.setInfo(StatusNotification.builder()
				.withConnectorId(cpc.getId().getConnectorId())
				.withStatus(ChargePointStatus.Available)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.now()).build());
		// @formatter:on
		ChargePointConnectorKey pk = dao.save(cpc);
		assertThat("PK preserved", pk, equalTo(cpc.getId()));
		last = cpc;
	}

	@Test
	public void getByPK() {
		insert();
		ChargePointConnector entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Connector info", entity.getInfo(), equalTo(last.getInfo()));
		// TODO
	}

	@Test
	public void update() {
		insert();
		ChargePointConnector cpc = dao.get(last.getId());
		cpc.setInfo(cpc.getInfo().toBuilder().withStatus(ChargePointStatus.Unavailable)
				.withVendorId(UUID.randomUUID().toString()).build());
		ChargePointConnectorKey pk = dao.save(cpc);
		assertThat("PK unchanged", pk, equalTo(cpc.getId()));

		ChargePointConnector entity = dao.get(pk);
		assertThat("Info status updated", entity.getInfo(), equalTo(cpc.getInfo()));
	}

	@Test
	public void findAll() {
		insert();

		List<ChargePointConnectorKey> expectedKeys = new ArrayList<>();
		expectedKeys.add(last.getId());

		// add another for same charge point
		ChargePointConnector cpc = new ChargePointConnector(
				new ChargePointConnectorKey(last.getId().getChargePointId(), 2), Instant.now());
		expectedKeys.add(cpc.getId());
		// @formatter:off
		cpc.setInfo(StatusNotification.builder()
				.withConnectorId(cpc.getId().getConnectorId())
				.withStatus(ChargePointStatus.Available)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.now()).build());
		// @formatter:on
		dao.save(cpc);

		// add another for a different charge point
		insert();

		expectedKeys.add(last.getId());
		expectedKeys.sort(null);

		Collection<ChargePointConnector> results = dao.getAll(null);
		assertThat("Result keys",
				results.stream().map(ChargePointConnector::getId).collect(Collectors.toList()),
				equalTo(expectedKeys));
	}

	@Test
	public void findForChargePoint() {
		insert();

		List<ChargePointConnectorKey> expectedKeys = new ArrayList<>();
		expectedKeys.add(last.getId());

		// add another for same charge point
		ChargePointConnector cpc = new ChargePointConnector(
				new ChargePointConnectorKey(last.getId().getChargePointId(), 2), Instant.now());
		expectedKeys.add(cpc.getId());
		// @formatter:off
		cpc.setInfo(StatusNotification.builder()
				.withConnectorId(cpc.getId().getConnectorId())
				.withStatus(ChargePointStatus.Available)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.now()).build());
		// @formatter:on
		dao.save(cpc);

		// add another for a different charge point
		insert();

		expectedKeys.sort(null);

		Collection<ChargePointConnector> results = dao
				.findByIdChargePointId(expectedKeys.get(0).getChargePointId());
		assertThat("Result keys",
				results.stream().map(ChargePointConnector::getId).collect(Collectors.toList()),
				equalTo(expectedKeys));
	}

	@Test
	public void postStoredEvent_insert() {
		EventAdmin eventAdmin = EasyMock.createMock(EventAdmin.class);
		dao.setEventAdmin(new StaticOptionalService<EventAdmin>(eventAdmin));

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// when
		replay(eventAdmin);
		insert();

		// then
		verify(eventAdmin);

		Event event = eventCaptor.getValue();
		assertThat("Event topic", event.getTopic(),
				equalTo("net/solarnetwork/dao/ChargePointConnector/STORED"));
		assertThat("Event prop ID", event.getProperty(GenericDao.ENTITY_EVENT_ENTITY_ID_PROPERTY),
				equalTo(last.getId()));
		assertThat("Event prop entity", event.getProperty(GenericDao.ENTITY_EVENT_ENTITY_PROPERTY),
				equalTo(last));
	}

	@Test
	public void postStoredEvent_update() {
		insert();

		EventAdmin eventAdmin = EasyMock.createMock(EventAdmin.class);
		dao.setEventAdmin(new StaticOptionalService<EventAdmin>(eventAdmin));

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// when
		replay(eventAdmin);
		ChargePointConnector cpc = dao.get(last.getId());
		cpc.setInfo(cpc.getInfo().toBuilder().withStatus(ChargePointStatus.Unavailable)
				.withVendorId(UUID.randomUUID().toString()).build());
		dao.save(cpc);

		// then
		verify(eventAdmin);

		Event event = eventCaptor.getValue();
		assertThat("Event topic", event.getTopic(),
				equalTo("net/solarnetwork/dao/ChargePointConnector/STORED"));
		assertThat("Event prop ID", event.getProperty(GenericDao.ENTITY_EVENT_ENTITY_ID_PROPERTY),
				equalTo(last.getId()));
		assertThat("Event prop entity", event.getProperty(GenericDao.ENTITY_EVENT_ENTITY_PROPERTY),
				equalTo(cpc));
		assertThat("Event prop entity is updated instance",
				((ChargePointConnector) event.getProperty(GenericDao.ENTITY_EVENT_ENTITY_PROPERTY))
						.isSameAs(cpc),
				equalTo(true));
	}

	@Test
	public void updateStatus() {
		// given
		insert();

		// when
		int result = dao.updateChargePointStatus(last.getId().getChargePointId(),
				last.getId().getConnectorId(), ChargePointStatus.Charging);

		// then
		assertThat("One row updated", result, equalTo(1));
		assertThat("Status updated", dao.get(last.getId()).getInfo().getStatus(),
				equalTo(ChargePointStatus.Charging));
	}

	@Test
	public void updateStatusForChargePoint() {
		// given
		insert();

		// add another for same charge point
		ChargePointConnector cpc = new ChargePointConnector(
				new ChargePointConnectorKey(last.getId().getChargePointId(), 2), Instant.now());

		// @formatter:off
		cpc.setInfo(StatusNotification.builder()
				.withConnectorId(cpc.getId().getConnectorId())
				.withStatus(ChargePointStatus.Available)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.now()).build());
		// @formatter:on
		dao.save(cpc);

		// add another for a different charge point, to verify we don't update this
		insert();

		// when
		int result = dao.updateChargePointStatus(cpc.getId().getChargePointId(), 0,
				ChargePointStatus.Charging);

		// then
		assertThat("Two row updated", result, equalTo(2));
		assertThat("Status updated for charge point",
				dao.findByIdChargePointId(cpc.getId().getChargePointId()).stream()
						.map(c -> c.getInfo().getStatus()).collect(Collectors.toList()),
				contains(ChargePointStatus.Charging, ChargePointStatus.Charging));
		assertThat("Other charge point status unchanged", dao.get(last.getId()).getInfo().getStatus(),
				equalTo(last.getInfo().getStatus()));
	}

}
