/* ==================================================================
 * JdbcChargeSessionDao.java - 10/02/2020 11:25:02 am
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
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.node.dao.jdbc.BaseJdbcGenericDao;
import net.solarnetwork.node.ocpp.dao.ChargeSessionDao;
import net.solarnetwork.node.ocpp.domain.ChargeSession;

/**
 * JDBC based implementation of {@link ChargeSessionDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargeSessionDao extends BaseJdbcGenericDao<ChargeSession, UUID>
		implements ChargeSessionDao {

	/** The table name for {@link ChargeSession} entities. */
	public static final String TABLE_NAME = "charge_sess";

	/** The charge point table version. */
	public static final int VERSION = 1;

	/**
	 * Constructor.
	 */
	public JdbcChargeSessionDao() {
		super(ChargeSession.class, UUID.class, new ChargeSessionRowMapper(), TABLE_NAME_TEMPALTE,
				TABLE_NAME, VERSION);
	}

	@Override
	protected void setStoreStatementValues(ChargeSession obj, PreparedStatement ps) throws SQLException {
		setUuidParameters(ps, 1, obj.getId());
		setInstantParameter(ps, 3, obj.getCreated() != null ? obj.getCreated() : Instant.now());
		ps.setString(4, obj.getAuthId());
		ps.setInt(5, obj.getConnectionId());
		setUpdateStatementValues(obj, ps, 5);
	}

	@Override
	protected void setUpdateStatementValues(ChargeSession obj, PreparedStatement ps)
			throws SQLException {
		setUpdateStatementValues(obj, ps, 0);
		setUuidParameters(ps, 3, obj.getId());
	}

	protected void setUpdateStatementValues(ChargeSession obj, PreparedStatement ps, int offset)
			throws SQLException {
		setInstantParameter(ps, 1 + offset, obj.getEnded());
		setInstantParameter(ps, 2 + offset, obj.getPosted());
	}

	/**
	 * A row mapper for {@link ChargeSession} entities.
	 */
	public static final class ChargeSessionRowMapper implements RowMapper<ChargeSession> {

		@Override
		public ChargeSession mapRow(ResultSet rs, int rowNum) throws SQLException {
			UUID id = getUuidColumns(rs, 1);
			Instant created = getInstantColumn(rs, 3);

			ChargeSession obj = new ChargeSession(id, created, rs.getString(4), rs.getInt(5));
			obj.setTransactionId(rs.getInt(6));
			obj.setEnded(getInstantColumn(rs, 7));
			obj.setPosted(getInstantColumn(rs, 8));

			return obj;
		}

	}
}
