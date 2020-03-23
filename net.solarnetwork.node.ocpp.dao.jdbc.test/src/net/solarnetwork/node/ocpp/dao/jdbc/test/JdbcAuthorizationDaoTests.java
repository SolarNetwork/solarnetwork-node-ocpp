/* ==================================================================
 * JdbcAuthorizationDaoTests.java - 7/02/2020 12:22:50 pm
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
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.UUID;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.ocpp.dao.jdbc.JdbcAuthorizationDao;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.ocpp.domain.Authorization;

/**
 * Test cases for the {@link JdbcAuthorizationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcAuthorizationDaoTests extends AbstractNodeTransactionalTest {

	@Resource(name = "dataSource")
	private DataSource dataSource;

	private JdbcAuthorizationDao dao;
	private Authorization last;

	@Before
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		dao = new JdbcAuthorizationDao();
		dao.setDataSource(dataSource);
		dao.init();
	}

	private Authorization createTestAuthorization(String vendor, String model) {
		Authorization auth = new Authorization(null,
				// note NOT Instant.ofEpochMilli(System.currentTimeMillis()) because database might not store nanoseconds
				Instant.ofEpochMilli(System.currentTimeMillis()));
		auth.setToken(UUID.randomUUID().toString().substring(0, 20));
		auth.setEnabled(true);
		auth.setExpiryDate(auth.getCreated().plus(1, ChronoUnit.HOURS));
		auth.setParentId(UUID.randomUUID().toString().substring(0, 20));
		return auth;
	}

	@Test
	public void insert() {
		Authorization auth = createTestAuthorization("foo", "bar");
		Long pk = dao.save(auth);
		assertThat("PK generated", pk, notNullValue());
		last = new Authorization(pk, auth.getCreated());
		last.setToken(auth.getToken());
		last.setEnabled(auth.isEnabled());
		last.setExpiryDate(auth.getExpiryDate());
		last.setParentId(auth.getParentId());
	}

	@Test
	public void getByPK() {
		insert();
		Authorization entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		// TODO
	}

	@Test
	public void update() {
		insert();
		Authorization auth = dao.get(last.getId());
		auth.setExpiryDate(auth.getExpiryDate().plus(1, ChronoUnit.HOURS));
		auth.setParentId(null);
		Long pk = dao.save(auth);
		assertThat("PK unchanged", pk, equalTo(auth.getId()));

		Authorization entity = dao.get(pk);
		assertThat("Expiry updated", entity.getExpiryDate(), equalTo(auth.getExpiryDate()));
		assertThat("Parent ID updated", entity.getParentId(), nullValue());
	}

	@Test
	public void findAll() {
		Authorization obj1 = createTestAuthorization("foo", "bar");
		obj1 = dao.get(dao.save(obj1));
		Authorization obj2 = new Authorization(obj1.getCreated().minusSeconds(60), "b");
		obj2 = dao.get(dao.save(obj2));
		Authorization obj3 = new Authorization(obj1.getCreated().plusSeconds(60), "c");
		obj3 = dao.get(dao.save(obj3));

		Collection<Authorization> results = dao.getAll(null);
		assertThat("Results found in order", results, contains(obj2, obj1, obj3));
	}

	@Test
	public void findByToken_none() {
		Authorization entity = dao.getForToken("foo");
		assertThat("No users", entity, nullValue());
	}

	@Test
	public void findByToken_noMatch() {
		insert();
		Authorization entity = dao.getForToken("not a match");
		assertThat("No match", entity, nullValue());
	}

	@Test
	public void findByToken() {
		findAll();
		Authorization entity = dao.getForToken("b");
		assertThat("Match", entity, notNullValue());
		assertThat("Token matches", entity.getToken(), equalTo("b"));
	}
}
