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
import net.solarnetwork.node.ocpp.dao.ChargePointDao;
import net.solarnetwork.node.ocpp.domain.ChargePoint;
import net.solarnetwork.node.ocpp.domain.ChargePointInfo;
import net.solarnetwork.node.ocpp.domain.RegistrationStatus;

/**
 * JDBC based implementation of {@link ChargePointDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargePointDao extends BaseJdbcGenericDao<ChargePoint, String>
		implements ChargePointDao {

	/** The table name for {@link ChargePoint} entities. */
	public static final String TABLE_NAME = "charge_point";

	/** The charge point table version. */
	public static final int VERSION = 1;

	/**
	 * Constructor.
	 */
	public JdbcChargePointDao() {
		super(ChargePoint.class, String.class, new ChargePointRowMapper(), TABLE_NAME_TEMPALTE,
				TABLE_NAME, VERSION);
	}

	@Override
	protected void setStoreStatementValues(ChargePoint obj, PreparedStatement ps) throws SQLException {
		ps.setString(1, obj.getId());
		setInstantParameter(ps, 2, obj.getCreated() != null ? obj.getCreated() : Instant.now());
		setUpdateStatementValues(obj, ps, 2);
	}

	@Override
	protected void setUpdateStatementValues(ChargePoint obj, PreparedStatement ps) throws SQLException {
		setUpdateStatementValues(obj, ps, 0);
		ps.setString(11, obj.getId());
	}

	protected void setUpdateStatementValues(ChargePoint obj, PreparedStatement ps, int offset)
			throws SQLException {
		ps.setBoolean(1 + offset, obj.isEnabled());
		ps.setInt(2 + offset,
				obj.getRegistrationStatus() != null ? obj.getRegistrationStatus().codeValue()
						: RegistrationStatus.Unknown.codeValue());

		ChargePointInfo info = obj.getInfo() != null ? obj.getInfo() : new ChargePointInfo();
		ps.setString(3 + offset, info.getChargePointVendor());
		ps.setString(4 + offset, info.getChargePointModel());
		ps.setString(5 + offset, info.getChargeBoxSerialNumber());
		ps.setString(6 + offset, info.getFirmwareVersion());
		ps.setString(7 + offset, info.getIccid());
		ps.setString(8 + offset, info.getImsi());
		ps.setString(9 + offset, info.getMeterType());
		ps.setString(10 + offset, info.getMeterSerialNumber());
	}

	/**
	 * A row mapper for {@link ChargePoint} entities.
	 */
	public static final class ChargePointRowMapper implements RowMapper<ChargePoint> {

		@Override
		public ChargePoint mapRow(ResultSet rs, int rowNum) throws SQLException {
			String id = rs.getString(1);
			Instant created = getInstantColumn(rs, 2);

			ChargePoint obj = new ChargePoint(id, created);
			obj.setEnabled(rs.getBoolean(3));
			obj.setRegistrationStatus(RegistrationStatus.forCode(rs.getInt(4)));

			ChargePointInfo info = new ChargePointInfo();
			info.setChargePointVendor(rs.getString(5));
			info.setChargePointModel(rs.getString(6));
			info.setChargeBoxSerialNumber(rs.getString(7));
			info.setFirmwareVersion(rs.getString(8));
			info.setIccid(rs.getString(9));
			info.setImsi(rs.getString(10));
			info.setMeterType(rs.getString(11));
			info.setMeterSerialNumber(rs.getString(12));
			obj.setInfo(info);

			return obj;
		}

	}

}
