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
import net.solarnetwork.node.ocpp.dao.AuthorizationDao;
import net.solarnetwork.node.ocpp.domain.Authorization;
import net.solarnetwork.node.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.node.ocpp.domain.AuthorizationStatus;

/**
 * JDBC based implementation of {@link AuthorizationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcAuthorizationDao extends BaseJdbcGenericDao<Authorization, String>
		implements AuthorizationDao {

	/** The table name for {@link Authorization} entities. */
	public static final String TABLE_NAME = "authorization";

	/** The charge point table version. */
	public static final int VERSION = 1;

	/**
	 * Constructor.
	 */
	public JdbcAuthorizationDao() {
		super(Authorization.class, String.class, new AuthorizationRowMapper(), TABLE_NAME_TEMPALTE,
				TABLE_NAME, VERSION);
	}

	@Override
	protected void setStoreStatementValues(Authorization obj, PreparedStatement ps) throws SQLException {
		ps.setObject(1, obj.getId());
		setInstantParameter(ps, 2, obj.getCreated() != null ? obj.getCreated() : Instant.now());
		setUpdateStatementValues(obj, ps, 2);
	}

	@Override
	protected void setUpdateStatementValues(Authorization obj, PreparedStatement ps)
			throws SQLException {
		setUpdateStatementValues(obj, ps, 0);
		ps.setObject(5, obj.getId());
	}

	protected void setUpdateStatementValues(Authorization obj, PreparedStatement ps, int offset)
			throws SQLException {
		ps.setBoolean(1 + offset, obj.isEnabled());

		AuthorizationInfo info = obj.getInfo();
		if ( info == null ) {
			throw new IllegalArgumentException("The info property must not be null.");
		}
		ps.setInt(2 + offset, info.getStatus() != null ? info.getStatus().codeValue()
				: AuthorizationStatus.Unknown.codeValue());
		setInstantParameter(ps, 3 + offset, info.getExpiryDate());
		ps.setString(4 + offset, info.getParentIdTag());
	}

	/**
	 * A row mapper for {@link Authorization} entities.
	 */
	public static final class AuthorizationRowMapper implements RowMapper<Authorization> {

		@Override
		public Authorization mapRow(ResultSet rs, int rowNum) throws SQLException {
			String id = rs.getString(1);
			Instant created = getInstantColumn(rs, 2);

			Authorization obj = new Authorization(id, created);
			obj.setEnabled(rs.getBoolean(3));

			AuthorizationInfo info = new AuthorizationInfo(id, AuthorizationStatus.forCode(rs.getInt(4)),
					getInstantColumn(rs, 5), rs.getString(6));
			obj.setInfo(info);

			return obj;
		}

	}

}
