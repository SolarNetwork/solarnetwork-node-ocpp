/* ==================================================================
 * JdbcChargePointConnectorConnectorDao.java - 12/02/2020 4:33:10 pm
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
import java.util.Collection;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.node.dao.jdbc.BaseJdbcGenericDao;
import net.solarnetwork.ocpp.dao.ChargePointConnectorDao;
import net.solarnetwork.ocpp.domain.ChargePointConnector;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargePointErrorCode;
import net.solarnetwork.ocpp.domain.ChargePointStatus;
import net.solarnetwork.ocpp.domain.StatusNotification;

/**
 * JDBC implementation of {@link ChargePointConnectorDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargePointConnectorDao
		extends BaseJdbcGenericDao<ChargePointConnector, ChargePointConnectorKey>
		implements ChargePointConnectorDao {

	/**
	 * The SQL resource suffix for finding all entities for a given Charge Point
	 * ID.
	 */
	public static final String SQL_FIND_BY_CHARGE_POINT = "find-for-charge-point";

	/**
	 * The SQL resource suffix for updating the status of a specific connector.
	 */
	public static final String SQL_UPDATE_STATUS = "update-status";

	/**
	 * The SQL resource suffix for updating the status of all connectors with a
	 * given Charge Point ID.
	 */
	public static final String SQL_UPDATE_STATUS_FOR_CHARGE_POINT = "update-status-for-charge-point";

	/** The table name for {@link ChargePointConnector} entities. */
	public static final String TABLE_NAME = "charge_point_conn";

	/** The charge point table version. */
	public static final int VERSION = 1;

	/**
	 * Constructor.
	 */
	public JdbcChargePointConnectorDao() {
		super(ChargePointConnector.class, ChargePointConnectorKey.class,
				new ChargePointConnectorRowMapper(), TABLE_NAME_TEMPALTE, TABLE_NAME, VERSION);
	}

	@Override
	public Collection<ChargePointConnector> findByChargePointId(long chargePointId) {
		return getJdbcTemplate().query(getSqlResource(SQL_FIND_BY_CHARGE_POINT), getRowMapper(),
				chargePointId);
	}

	@Override
	public ChargePointConnectorKey saveStatusInfo(long chargePointId, StatusNotification info) {
		ChargePointConnectorKey pk = new ChargePointConnectorKey(chargePointId, info.getConnectorId());
		ChargePointConnector entity = get(pk);
		if ( entity == null ) {
			entity = new ChargePointConnector(pk, Instant.now());
		} else if ( info.isSameAs(entity.getInfo()) ) {
			return pk;
		}
		entity.setInfo(info);
		return save(entity);
	}

	@Override
	public int updateChargePointStatus(long chargePointId, int connectorId, ChargePointStatus status) {
		if ( connectorId < 1 ) {
			return getJdbcTemplate().update(getSqlResource(SQL_UPDATE_STATUS_FOR_CHARGE_POINT),
					status.getCode(), chargePointId);
		}
		return getJdbcTemplate().update(getSqlResource(SQL_UPDATE_STATUS), status.getCode(),
				chargePointId, connectorId);
	}

	@Override
	protected Object[] primaryKeyArguments(ChargePointConnectorKey id) {
		return new Object[] { id.getChargePointId(), id.getConnectorId() };
	}

	@Override
	protected void setStoreStatementValues(ChargePointConnector obj, PreparedStatement ps)
			throws SQLException {
		ps.setLong(1, obj.getId().getChargePointId());
		ps.setInt(2, obj.getId().getConnectorId());
		setInstantParameter(ps, 3, obj.getCreated() != null ? obj.getCreated() : Instant.now());
		setUpdateStatementValues(obj, ps, 3);
	}

	@Override
	protected void setUpdateStatementValues(ChargePointConnector obj, PreparedStatement ps)
			throws SQLException {
		setUpdateStatementValues(obj, ps, 0);
		ps.setLong(7, obj.getId().getChargePointId());
		ps.setInt(8, obj.getId().getConnectorId());
	}

	protected void setUpdateStatementValues(ChargePointConnector obj, PreparedStatement ps, int offset)
			throws SQLException {
		StatusNotification info = obj.getInfo() != null ? obj.getInfo()
				: StatusNotification.builder().build();
		ps.setInt(1 + offset, info.getStatus() != null ? info.getStatus().getCode()
				: ChargePointStatus.Unknown.getCode());
		ps.setInt(2 + offset, info.getErrorCode() != null ? info.getErrorCode().getCode()
				: ChargePointErrorCode.Unknown.getCode());
		setInstantParameter(ps, 3 + offset, info.getTimestamp());
		ps.setString(4 + offset, info.getInfo());
		ps.setString(5 + offset, info.getVendorId());
		ps.setString(6 + offset, info.getVendorErrorCode());
	}

	/**
	 * A row mapper for {@link ChargePointConnector} entities.
	 */
	public static final class ChargePointConnectorRowMapper implements RowMapper<ChargePointConnector> {

		@Override
		public ChargePointConnector mapRow(ResultSet rs, int rowNum) throws SQLException {
			ChargePointConnectorKey id = new ChargePointConnectorKey(rs.getLong(1), rs.getInt(2));
			Instant created = getInstantColumn(rs, 3);

			ChargePointConnector obj = new ChargePointConnector(id, created);
			// @formatter:off
			obj.setInfo(StatusNotification.builder()
					.withConnectorId(id.getConnectorId())
					.withStatus(ChargePointStatus.forCode(rs.getInt(4)))
					.withErrorCode(ChargePointErrorCode.forCode(rs.getInt(5)))
					.withTimestamp(getInstantColumn(rs, 6))
					.withInfo(rs.getString(7))
					.withVendorId(rs.getString(8))
					.withVendorErrorCode(rs.getString(9))
					.build());
			// @formatter:on
			return obj;
		}

	}

}
