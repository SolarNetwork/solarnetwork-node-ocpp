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
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.node.dao.jdbc.BaseJdbcGenericDao;
import net.solarnetwork.ocpp.dao.ChargeSessionDao;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.ocpp.domain.ChargeSessionEndReason;
import net.solarnetwork.ocpp.domain.Location;
import net.solarnetwork.ocpp.domain.Measurand;
import net.solarnetwork.ocpp.domain.Phase;
import net.solarnetwork.ocpp.domain.ReadingContext;
import net.solarnetwork.ocpp.domain.SampledValue;
import net.solarnetwork.ocpp.domain.UnitOfMeasure;

/**
 * JDBC based implementation of {@link ChargeSessionDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargeSessionDao extends BaseJdbcGenericDao<ChargeSession, UUID>
		implements ChargeSessionDao {

	public enum SqlResource {

		/**
		 * Delete entities with a {@code posted} date older than a given date.
		 */
		DeleteByPosted("delete-for-posted"),

		/**
		 * Find entities for a given charge point ID and transaction ID and a
		 * {@literal null} {@code ended} value.
		 */
		FindByIncompleteTransaction("find-for-incomplete-tx"),

		/**
		 * Find entities for a given charge point ID and connector ID and a
		 * {@literal null} {@code ended} value.
		 */
		FindByIncompleteConnector("find-for-incomplete-conn"),

		/**
		 * Find entities for a given charge point ID and a {@literal null}
		 * {@code ended} value.
		 */
		FindByIncompleteChargePoint("find-for-incomplete-cp"),

		/** Find entities with a {@literal null} {@code ended} value. */
		FindByIncomplete("find-for-incomplete"),

		/** Find all sampled value readings for a charge session ID. */
		FindReadingBySession("find-reading-for-session"),

		/** Insert a sampled value reading. */
		InsertReading("insert-reading");

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

	/** The table name for {@link ChargeSession} entities. */
	public static final String TABLE_NAME = "charge_sess";

	/** The charge point table version. */
	public static final int VERSION = 1;

	private static final RowMapper<SampledValue> READING_ROW_MAPPER = new ReadingRowMapper();

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
		ps.setLong(5, obj.getChargePointId());
		ps.setInt(6, obj.getConnectorId());
		setUpdateStatementValues(obj, ps, 6);
	}

	@Override
	public ChargeSession getIncompleteChargeSessionForTransaction(long chargePointId,
			int transactionId) {
		return findFirst(getSqlResource(SqlResource.FindByIncompleteTransaction.getResource()),
				chargePointId, transactionId);
	}

	@Override
	public ChargeSession getIncompleteChargeSessionForConnector(long chargePointId, int connectorId) {
		return findFirst(getSqlResource(SqlResource.FindByIncompleteConnector.getResource()),
				chargePointId, connectorId);
	}

	@Override
	public Collection<ChargeSession> getIncompleteChargeSessionForChargePoint(long chargePointId) {
		return getJdbcTemplate().query(
				getSqlResource(SqlResource.FindByIncompleteChargePoint.getResource()), getRowMapper(),
				chargePointId);
	}

	@Override
	public Collection<ChargeSession> getIncompleteChargeSessions() {
		return getJdbcTemplate().query(getSqlResource(SqlResource.FindByIncomplete.getResource()),
				getRowMapper());
	}

	@Override
	public int deletePostedChargeSessions(Instant expirationDate) {
		return getJdbcTemplate().update(getSqlResource(SqlResource.DeleteByPosted.getResource()),
				new PreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps) throws SQLException {
						setInstantParameter(ps, 1,
								expirationDate != null ? expirationDate : Instant.now());
					}

				});
	}

	@Override
	public void addReadings(Iterable<SampledValue> readings) {
		if ( readings == null ) {
			return;
		}
		getJdbcTemplate().execute(getSqlResource(SqlResource.InsertReading.getResource()),
				new PreparedStatementCallback<Object>() {

					@Override
					public Object doInPreparedStatement(PreparedStatement ps)
							throws SQLException, DataAccessException {
						for ( SampledValue v : readings ) {
							setUuidParameters(ps, 1, v.getSessionId());
							setInstantParameter(ps, 3, v.getTimestamp());
							ps.setInt(4, v.getLocation() != null ? v.getLocation().codeValue()
									: Location.Outlet.codeValue());
							ps.setInt(5, v.getUnit() != null ? v.getUnit().codeValue()
									: UnitOfMeasure.Unknown.codeValue());
							ps.setInt(6, v.getContext() != null ? v.getContext().codeValue()
									: ReadingContext.Unknown.codeValue());
							ps.setInt(7, v.getMeasurand() != null ? v.getMeasurand().codeValue()
									: Measurand.Unknown.codeValue());
							ps.setObject(8,
									v.getPhase() != null && v.getPhase() != Phase.Unknown
											? v.getPhase().codeValue()
											: null);
							ps.setString(9, v.getValue());
							ps.executeUpdate();
						}
						return null;
					}
				});
	}

	public static final class ReadingRowMapper implements RowMapper<SampledValue> {

		@Override
		public SampledValue mapRow(ResultSet rs, int rowNum) throws SQLException {
			// @formatter:off
			SampledValue.Builder result = SampledValue.builder()
					.withSessionId(getUuidColumns(rs, 1))
					.withTimestamp(getInstantColumn(rs, 3))
					.withLocation(Location.forCode(rs.getInt(4)))
					.withUnit(UnitOfMeasure.forCode(rs.getInt(5)))
					.withContext(ReadingContext.forCode(rs.getInt(6)))
					.withMeasurand(Measurand.forCode(rs.getInt(7)))
					.withValue(rs.getString(9));
			// @formatter:onf
			int phase = rs.getInt(8);
			if ( !rs.wasNull() ) {
				result.withPhase(Phase.forCode(phase));
			}
			return result.build();
		}

	}

	@Override
	public List<SampledValue> findReadingsForSession(UUID sessionId) {
		return getJdbcTemplate().query(getSqlResource(SqlResource.FindReadingBySession.getResource()),
				new Object[] { sessionId.getMostSignificantBits(), sessionId.getLeastSignificantBits() },
				READING_ROW_MAPPER);
	}

	@Override
	protected void setUpdateStatementValues(ChargeSession obj, PreparedStatement ps)
			throws SQLException {
		setUpdateStatementValues(obj, ps, 0);
		setUuidParameters(ps, 5, obj.getId());
	}

	protected void setUpdateStatementValues(ChargeSession obj, PreparedStatement ps, int offset)
			throws SQLException {
		setInstantParameter(ps, 1 + offset, obj.getEnded());
		ps.setInt(2 + offset, obj.getEndReason() != null ? obj.getEndReason().codeValue()
				: ChargeSessionEndReason.Unknown.codeValue());
		ps.setString(3 + offset, obj.getEndAuthId());
		setInstantParameter(ps, 4 + offset, obj.getPosted());
	}

	/**
	 * A row mapper for {@link ChargeSession} entities.
	 */
	public static final class ChargeSessionRowMapper implements RowMapper<ChargeSession> {

		@Override
		public ChargeSession mapRow(ResultSet rs, int rowNum) throws SQLException {
			UUID id = getUuidColumns(rs, 1);
			Instant created = getInstantColumn(rs, 3);

			ChargeSession obj = new ChargeSession(id, created, rs.getString(4), rs.getLong(5),
					rs.getInt(6), rs.getInt(7));
			obj.setEnded(getInstantColumn(rs, 8));
			obj.setEndReason(ChargeSessionEndReason.forCode(rs.getInt(9)));
			obj.setEndAuthId(rs.getString(10));
			obj.setPosted(getInstantColumn(rs, 11));

			return obj;
		}

	}
}
