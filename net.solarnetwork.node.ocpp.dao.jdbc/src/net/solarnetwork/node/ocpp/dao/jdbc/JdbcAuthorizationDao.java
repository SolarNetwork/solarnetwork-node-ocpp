/* ==================================================================
 * JdbcAuthorizationDao.java - 9/02/2020 5:06:25 pm
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

package net.solarnetwork.node.ocpp.dao.jdbc;

import static net.solarnetwork.node.ocpp.dao.jdbc.Constants.TABLE_NAME_TEMPALTE;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.node.dao.jdbc.BaseJdbcGenericDao;
import net.solarnetwork.ocpp.dao.AuthorizationDao;
import net.solarnetwork.ocpp.domain.Authorization;

/**
 * JDBC based implementation of {@link AuthorizationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcAuthorizationDao extends BaseJdbcGenericDao<Authorization, Long>
		implements AuthorizationDao {

	/**
	 * Enumeration of SQL resources.
	 */
	public enum SqlResource {

		/** Find by token. */
		GetByToken("get-token");

		private final String resource;

		private SqlResource(String resource) {
			this.resource = resource;
		}

		/**
		 * Get the SQL resource name.
		 * 
		 * @return the resource
		 */
		public String getResource() {
			return resource;
		}
	}

	/** The table name for {@link Authorization} entities. */
	public static final String TABLE_NAME = "authorization";

	/** The charge point table version. */
	public static final int VERSION = 1;

	/**
	 * Constructor.
	 */
	public JdbcAuthorizationDao() {
		super(Authorization.class, Long.class, new AuthorizationRowMapper(), TABLE_NAME_TEMPALTE,
				TABLE_NAME, VERSION);
		setUseAutogeneratedKeys(true);
	}

	@Override
	public Authorization getForToken(String token) {
		return findFirst(getSqlResource(SqlResource.GetByToken.getResource()), token);
	}

	@Override
	protected void setStoreStatementValues(Authorization obj, PreparedStatement ps) throws SQLException {
		setInstantParameter(ps, 1, obj.getCreated() != null ? obj.getCreated() : Instant.now());
		setUpdateStatementValues(obj, ps, 1);
	}

	@Override
	protected void setUpdateStatementValues(Authorization obj, PreparedStatement ps)
			throws SQLException {
		setUpdateStatementValues(obj, ps, 0);
		ps.setObject(5, obj.getId());
	}

	protected void setUpdateStatementValues(Authorization obj, PreparedStatement ps, int offset)
			throws SQLException {
		ps.setString(1 + offset, obj.getToken());
		ps.setBoolean(2 + offset, obj.isEnabled());
		setInstantParameter(ps, 3 + offset, obj.getExpiryDate());
		ps.setString(4 + offset, obj.getParentId());
	}

	/**
	 * A row mapper for {@link Authorization} entities.
	 */
	public static final class AuthorizationRowMapper implements RowMapper<Authorization> {

		@Override
		public Authorization mapRow(ResultSet rs, int rowNum) throws SQLException {
			Long id = rs.getLong(1);
			Instant created = getInstantColumn(rs, 2);

			Authorization obj = new Authorization(id, created);
			obj.setToken(rs.getString(3));
			obj.setEnabled(rs.getBoolean(4));
			obj.setExpiryDate(getInstantColumn(rs, 5));
			obj.setParentId(rs.getString(6));

			return obj;
		}

	}

}