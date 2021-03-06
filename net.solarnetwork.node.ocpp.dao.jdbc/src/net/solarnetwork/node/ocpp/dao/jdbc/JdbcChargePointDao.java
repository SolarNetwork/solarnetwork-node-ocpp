/* ==================================================================
 * JdbcChargePointDao.java - 7/02/2020 9:53:08 am
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
import net.solarnetwork.ocpp.dao.ChargePointDao;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.RegistrationStatus;

/**
 * JDBC based implementation of {@link ChargePointDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargePointDao extends BaseJdbcGenericDao<ChargePoint, Long> implements ChargePointDao {

	/**
	 * Enumeration of SQL resources.
	 */
	public enum SqlResource {

		/** Find by token. */
		GetByIdentifier("get-ident");

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

	/** The table name for {@link ChargePoint} entities. */
	public static final String TABLE_NAME = "charge_point";

	/** The charge point table version. */
	public static final int VERSION = 1;

	/**
	 * Constructor.
	 */
	public JdbcChargePointDao() {
		super(ChargePoint.class, Long.class, new ChargePointRowMapper(), TABLE_NAME_TEMPALTE, TABLE_NAME,
				VERSION);
		setUseAutogeneratedKeys(true);
	}

	@Override
	public ChargePoint getForIdentity(ChargePointIdentity identity) {
		return findFirst(getSqlResource(SqlResource.GetByIdentifier.getResource()),
				identity.getIdentifier());
	}

	@Override
	protected void setStoreStatementValues(ChargePoint obj, PreparedStatement ps) throws SQLException {
		setInstantParameter(ps, 1, obj.getCreated() != null ? obj.getCreated() : Instant.now());
		setUpdateStatementValues(obj, ps, 1);
	}

	@Override
	protected void setUpdateStatementValues(ChargePoint obj, PreparedStatement ps) throws SQLException {
		setUpdateStatementValues(obj, ps, 0);
		ps.setObject(14, obj.getId());
	}

	protected void setUpdateStatementValues(ChargePoint obj, PreparedStatement ps, int offset)
			throws SQLException {
		ps.setBoolean(1 + offset, obj.isEnabled());
		ps.setInt(2 + offset, obj.getRegistrationStatus() != null ? obj.getRegistrationStatus().getCode()
				: RegistrationStatus.Unknown.getCode());

		ChargePointInfo info = obj.getInfo() != null ? obj.getInfo() : new ChargePointInfo();
		ps.setString(3 + offset, info.getId());
		ps.setString(4 + offset, info.getChargePointVendor());
		ps.setString(5 + offset, info.getChargePointModel());
		ps.setString(6 + offset, info.getChargePointSerialNumber());
		ps.setString(7 + offset, info.getChargeBoxSerialNumber());
		ps.setString(8 + offset, info.getFirmwareVersion());
		ps.setString(9 + offset, info.getIccid());
		ps.setString(10 + offset, info.getImsi());
		ps.setString(11 + offset, info.getMeterType());
		ps.setString(12 + offset, info.getMeterSerialNumber());
		ps.setInt(13 + offset, obj.getConnectorCount());
	}

	/**
	 * A row mapper for {@link ChargePoint} entities.
	 */
	public static final class ChargePointRowMapper implements RowMapper<ChargePoint> {

		@Override
		public ChargePoint mapRow(ResultSet rs, int rowNum) throws SQLException {
			Long id = rs.getLong(1);
			Instant created = getInstantColumn(rs, 2);

			ChargePointInfo info = new ChargePointInfo(rs.getString(5));
			info.setChargePointVendor(rs.getString(6));
			info.setChargePointModel(rs.getString(7));
			info.setChargePointSerialNumber(rs.getString(8));
			info.setChargeBoxSerialNumber(rs.getString(9));
			info.setFirmwareVersion(rs.getString(10));
			info.setIccid(rs.getString(11));
			info.setImsi(rs.getString(12));
			info.setMeterType(rs.getString(13));
			info.setMeterSerialNumber(rs.getString(14));

			ChargePoint obj = new ChargePoint(id, created, info);
			obj.setEnabled(rs.getBoolean(3));
			obj.setRegistrationStatus(RegistrationStatus.forCode(rs.getInt(4)));
			obj.setConnectorCount(rs.getInt(15));

			return obj;
		}

	}

}
