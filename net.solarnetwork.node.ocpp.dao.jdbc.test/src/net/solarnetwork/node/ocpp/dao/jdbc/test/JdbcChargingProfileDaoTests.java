/* ==================================================================
 * JdbcChargingProfileDaoTests.java - 18/02/2020 8:53:14 pm
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
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.ocpp.dao.jdbc.JdbcChargingProfileDao;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.ocpp.domain.ChargingProfile;
import net.solarnetwork.ocpp.domain.ChargingProfileInfo;
import net.solarnetwork.ocpp.domain.ChargingProfileKind;
import net.solarnetwork.ocpp.domain.ChargingProfilePurpose;
import net.solarnetwork.ocpp.domain.ChargingScheduleInfo;
import net.solarnetwork.ocpp.domain.ChargingSchedulePeriodInfo;
import net.solarnetwork.ocpp.domain.ChargingScheduleRecurrency;
import net.solarnetwork.ocpp.domain.UnitOfMeasure;

/**
 * Test cases for the {@link JdbcChargingProfileDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargingProfileDaoTests extends AbstractNodeTransactionalTest {

	@Resource(name = "dataSource")
	private DataSource dataSource;

	private JdbcChargingProfileDao dao;
	private ChargingProfile last;

	@Before
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		dao = new JdbcChargingProfileDao();
		dao.setDataSource(dataSource);
		dao.init();
	}

	private ChargingProfile createTestChargingProfile() {
		ZonedDateTime start = LocalDateTime.of(2020, 2, 17, 0, 0).atZone(ZoneId.systemDefault());
		ChargingScheduleInfo schedule = new ChargingScheduleInfo(Duration.ofHours(24), start.toInstant(),
				UnitOfMeasure.W, BigDecimal.ONE.setScale(1));
		ChargingProfileInfo info = new ChargingProfileInfo(ChargingProfilePurpose.ChargePointMaxProfile,
				ChargingProfileKind.Recurring, schedule);
		info.setRecurrency(ChargingScheduleRecurrency.Daily);
		ChargingProfile sess = new ChargingProfile(UUID.randomUUID(),
				Instant.ofEpochMilli(System.currentTimeMillis()), info);
		return sess;
	}

	@Test
	public void insert() {
		ChargingProfile entity = createTestChargingProfile();
		UUID pk = dao.save(entity);
		assertThat("PK preserved", pk, equalTo(entity.getId()));
		last = entity;
	}

	@Test
	public void insert_withPeriods() {
		ChargingProfile entity = createTestChargingProfile();
		entity.getInfo().getSchedule()
				.addPeriod(new ChargingSchedulePeriodInfo(Duration.ZERO, new BigDecimal("1.1")));
		entity.getInfo().getSchedule()
				.addPeriod(new ChargingSchedulePeriodInfo(Duration.ofHours(1), new BigDecimal("2.1")));
		entity.getInfo().getSchedule()
				.addPeriod(new ChargingSchedulePeriodInfo(Duration.ofHours(2), new BigDecimal("3.1")));
		UUID pk = dao.save(entity);
		assertThat("PK preserved", pk, equalTo(entity.getId()));
		last = entity;
	}

	@Test
	public void getByPK() {
		insert();
		ChargingProfile entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Purpose", entity.getInfo().getPurpose(), equalTo(last.getInfo().getPurpose()));
		assertThat("Kind", entity.getInfo().getKind(), equalTo(last.getInfo().getKind()));
		assertThat("Recurrency", entity.getInfo().getRecurrency(),
				equalTo(last.getInfo().getRecurrency()));
		assertThat("Schedule", entity.getInfo().getSchedule().isSameAs(last.getInfo().getSchedule()),
				equalTo(true));
	}

	@Test
	public void getByPK_withPeriods() {
		insert_withPeriods();
		ChargingProfile entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Schedule", entity.getInfo().getSchedule().isSameAs(last.getInfo().getSchedule()),
				equalTo(true));
	}

	@Test
	public void update() {
		insert();
		ChargingProfile profile = dao.get(last.getId());
		profile.getInfo().setRecurrency(ChargingScheduleRecurrency.Weekly);
		profile.getInfo().setValidFrom(Instant.ofEpochMilli(System.currentTimeMillis()));
		profile.getInfo().setValidTo(profile.getInfo().getValidFrom().plusSeconds(60));
		profile.getInfo().getSchedule().setDuration(Duration.ofHours(1));
		profile.getInfo().getSchedule().setMinRate(new BigDecimal("12345.6"));
		UUID pk = dao.save(profile);
		assertThat("PK unchanged", pk, equalTo(profile.getId()));

		ChargingProfile entity = dao.get(pk);
		assertThat("Entity updated", entity.isSameAs(profile), equalTo(true));
	}

	@Test
	public void findAll() {
		ChargingProfile prof1 = createTestChargingProfile();
		dao.save(prof1);
		ChargingProfile prof2 = new ChargingProfile(UUID.randomUUID(), Instant.now().minusSeconds(60),
				new ChargingProfileInfo(prof1.getInfo()));
		dao.save(prof2);
		ChargingProfile prof3 = new ChargingProfile(UUID.randomUUID(), Instant.now().minusSeconds(30),
				new ChargingProfileInfo(prof1.getInfo()));
		dao.save(prof3);

		Collection<ChargingProfile> results = dao.getAll(null);
		assertThat("Results found in order", results, contains(prof2, prof3, prof1));
	}

	@Test
	public void findAll_sortByCreatedDesc() {
		ChargingProfile prof1 = createTestChargingProfile();
		dao.save(prof1);
		ChargingProfile prof2 = new ChargingProfile(UUID.randomUUID(), Instant.now().minusSeconds(60),
				new ChargingProfileInfo(prof1.getInfo()));
		dao.save(prof2);
		ChargingProfile prof3 = new ChargingProfile(UUID.randomUUID(), Instant.now().minusSeconds(30),
				new ChargingProfileInfo(prof1.getInfo()));
		dao.save(prof3);

		Collection<ChargingProfile> results = dao.getAll(GenericDao.SORT_BY_CREATED_DESCENDING);
		assertThat("Results found in order", results, contains(prof1, prof3, prof2));
	}

	@Test
	public void findAll_withPeriods() {
		ChargingProfile prof1 = createTestChargingProfile();
		prof1.getInfo().getSchedule()
				.addPeriod(new ChargingSchedulePeriodInfo(Duration.ZERO, new BigDecimal("11.1")));
		prof1.getInfo().getSchedule()
				.addPeriod(new ChargingSchedulePeriodInfo(Duration.ofHours(1), new BigDecimal("22.2")));
		dao.save(prof1);

		ChargingProfile prof2 = new ChargingProfile(UUID.randomUUID(), Instant.now().minusSeconds(60),
				new ChargingProfileInfo(prof1.getInfo()));
		dao.save(prof2);

		ChargingProfile prof3 = new ChargingProfile(UUID.randomUUID(), Instant.now().minusSeconds(30),
				new ChargingProfileInfo(prof1.getInfo()));
		prof3.getInfo().getSchedule()
				.addPeriod(new ChargingSchedulePeriodInfo(Duration.ZERO, new BigDecimal("33.3")));
		dao.save(prof3);

		List<ChargingProfile> results = dao.getAll(null).stream().collect(Collectors.toList());
		assertThat("Results found in order", results, contains(prof2, prof3, prof1));

		assertThat("Result 0 same", results.get(0).isSameAs(prof2), equalTo(true));
		assertThat("Result 1 same", results.get(1).isSameAs(prof3), equalTo(true));
		assertThat("Result 2 same", results.get(2).isSameAs(prof1), equalTo(true));
	}

}
