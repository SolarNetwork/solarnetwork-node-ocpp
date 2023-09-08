/* ==================================================================
 * JdbcChargingProfileDao.java - 18/02/2020 4:48:46 pm
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
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.node.dao.jdbc.BaseJdbcGenericDao;
import net.solarnetwork.ocpp.dao.ChargingProfileDao;
import net.solarnetwork.ocpp.domain.ChargingProfile;
import net.solarnetwork.ocpp.domain.ChargingProfileInfo;
import net.solarnetwork.ocpp.domain.ChargingProfileKind;
import net.solarnetwork.ocpp.domain.ChargingProfilePurpose;
import net.solarnetwork.ocpp.domain.ChargingScheduleInfo;
import net.solarnetwork.ocpp.domain.ChargingSchedulePeriodInfo;
import net.solarnetwork.ocpp.domain.ChargingScheduleRecurrency;
import net.solarnetwork.ocpp.domain.UnitOfMeasure;

/**
 * JDBC implementation of {@link ChargingProfileDao}.
 * 
 * @author matt
 * @version 2.0
 */
public class JdbcChargingProfileDao extends BaseJdbcGenericDao<ChargingProfile, UUID>
		implements ChargingProfileDao {

	/**
	 * Enumeration of SQL resources.
	 */
	public enum SqlResource {

		/** Insert a period. */
		InsertPeriod("insert-period"),

		/** Update a period. */
		UpdatePeriod("update-period"),

		/** Delete periods with an index over a given value. */
		DeletePeriodsOver("delete-period-over");

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

	/** The table name for {@link ChargingProfile} entities. */
	public static final String TABLE_NAME = "charge_prof";

	/** The charge point table version. */
	public static final int VERSION = 1;

	private final ResultSetExtractor<List<ChargingProfile>> PROFILE_EXTRACTOR = new ChargingProfileResultSetExtractor();

	/**
	 * Constructor.
	 */
	public JdbcChargingProfileDao() {
		super(ChargingProfile.class, UUID.class, null, TABLE_NAME_TEMPALTE, TABLE_NAME, VERSION);
	}

	@Override
	protected void insertDomainObject(ChargingProfile obj, String sqlInsert) {
		super.insertDomainObject(obj, sqlInsert);
		insertPeriods(obj);
	}

	private void insertPeriods(ChargingProfile obj) {
		if ( obj.getInfo() == null || obj.getInfo().getSchedule() == null ) {
			return;
		}
		List<ChargingSchedulePeriodInfo> periods = obj.getInfo().getSchedule().getPeriods();
		if ( periods == null || periods.isEmpty() ) {
			return;
		}
		getJdbcTemplate().batchUpdate(getSqlResource(SqlResource.InsertPeriod.getResource()),
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						ChargingSchedulePeriodInfo period = periods.get(i);
						setUuidParameters(ps, 1, obj.getId());
						ps.setInt(3, i);
						setUpdateStatementValues(period, ps, 3);
					}

					@Override
					public int getBatchSize() {
						return periods.size();
					}
				});

	}

	private void setUpdateStatementValues(ChargingSchedulePeriodInfo obj, PreparedStatement ps,
			int offset) throws SQLException {
		ps.setInt(1 + offset, obj.getStartOffsetSeconds());
		ps.setBigDecimal(2 + offset, obj.getRateLimit());
		ps.setObject(3 + offset, obj.getNumPhases());
	}

	@Override
	protected int updateDomainObject(ChargingProfile obj, String sqlUpdate) {
		int result = super.updateDomainObject(obj, sqlUpdate);
		if ( result > 0 ) {
			updatePeriods(obj);
		}
		return result;
	}

	private void updatePeriods(ChargingProfile obj) {
		List<ChargingSchedulePeriodInfo> periods = (obj.getInfo() != null
				&& obj.getInfo().getSchedule() != null
				&& obj.getInfo().getSchedule().getPeriods() != null
						? obj.getInfo().getSchedule().getPeriods()
						: Collections.emptyList());
		for ( ListIterator<ChargingSchedulePeriodInfo> itr = periods.listIterator(); itr.hasNext(); ) {
			ChargingSchedulePeriodInfo period = itr.next();
			int count = getJdbcTemplate().update(getSqlResource(SqlResource.UpdatePeriod.getResource()),
					new PreparedStatementSetter() {

						@Override
						public void setValues(PreparedStatement ps) throws SQLException {
							setUpdateStatementValues(period, ps, 0);
							setUuidParameters(ps, 4, obj.getId());
							ps.setInt(6, itr.previousIndex());
						}

					});
			if ( count < 1 ) {
				getJdbcTemplate().update(getSqlResource(SqlResource.InsertPeriod.getResource()),
						new PreparedStatementSetter() {

							@Override
							public void setValues(PreparedStatement ps) throws SQLException {
								setUuidParameters(ps, 1, obj.getId());
								ps.setInt(3, itr.previousIndex());
								setUpdateStatementValues(period, ps, 3);
							}

						});
			}
		}
		getJdbcTemplate().update(getSqlResource(SqlResource.DeletePeriodsOver.getResource()),
				obj.getId().getMostSignificantBits(), obj.getId().getLeastSignificantBits(),
				periods.size());
	}

	@Override
	protected void setStoreStatementValues(ChargingProfile obj, PreparedStatement ps)
			throws SQLException {
		setUuidParameters(ps, 1, obj.getId());
		setInstantParameter(ps, 3, obj.getCreated() != null ? obj.getCreated() : Instant.now());
		setUpdateStatementValues(obj, ps, 3);
	}

	@Override
	protected void setUpdateStatementValues(ChargingProfile obj, PreparedStatement ps)
			throws SQLException {
		setUpdateStatementValues(obj, ps, 0);
		setUuidParameters(ps, 10, obj.getId());
	}

	protected void setUpdateStatementValues(ChargingProfile obj, PreparedStatement ps, int offset)
			throws SQLException {
		ChargingProfileInfo info = obj.getInfo();
		ps.setInt(1 + offset, info.getPurpose() != null ? info.getPurpose().getCode()
				: ChargingProfilePurpose.Unknown.getCode());
		ps.setInt(2 + offset, info.getKind() != null ? info.getKind().getCode()
				: ChargingProfileKind.Unknown.getCode());
		ps.setInt(3 + offset, info.getRecurrency() != null ? info.getRecurrency().getCode()
				: ChargingScheduleRecurrency.Unknown.getCode());
		setInstantParameter(ps, 4 + offset, info.getValidFrom());
		setInstantParameter(ps, 5 + offset, info.getValidTo());

		ChargingScheduleInfo sched = info.getSchedule();
		ps.setObject(6 + offset, sched.getDuration() != null ? sched.getDuration().getSeconds() : null);
		setInstantParameter(ps, 7 + offset, sched.getStart());
		ps.setInt(8 + offset, sched.getRateUnit() != null ? sched.getRateUnit().getCode()
				: UnitOfMeasure.Unknown.getCode());
		ps.setBigDecimal(9 + offset, sched.getMinRate());
	}

	@Override
	protected ChargingProfile findFirst(String sql, Object... parameters) {
		List<ChargingProfile> results = getJdbcTemplate().query(sql, PROFILE_EXTRACTOR, parameters);
		return (results != null && !results.isEmpty() ? results.get(0) : null);
	}

	@Override
	public Collection<ChargingProfile> getAll(List<SortDescriptor> sorts) {
		return getJdbcTemplate().query(querySql(SQL_FIND_ALL, sorts), PROFILE_EXTRACTOR);
	}

	/**
	 * A {@link RowMapper} for periods.
	 */
	public static final class PeriodRowMapper implements RowMapper<ChargingSchedulePeriodInfo> {

		private final int offset;

		public PeriodRowMapper(int offset) {
			super();
			this.offset = offset;
		}

		@Override
		public ChargingSchedulePeriodInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			UUID profileId = getUuidColumns(rs, 1 + offset);
			if ( profileId == null ) {
				return null;
			}
			Duration startOffset = Duration.ofSeconds(rs.getInt(3 + offset));
			BigDecimal rateLimit = rs.getBigDecimal(4 + offset);
			ChargingSchedulePeriodInfo p = new ChargingSchedulePeriodInfo(startOffset, rateLimit);
			int numPhases = rs.getInt(5 + offset);
			if ( !rs.wasNull() ) {
				p.setNumPhases(numPhases);
			}
			return p;
		}

	}

	/**
	 * A {@link RowMapper} for charging profiles.
	 */
	public static final class ChargingProfileRowMapper implements RowMapper<ChargingProfile> {

		@Override
		public ChargingProfile mapRow(ResultSet rs, int rowNum) throws SQLException {
			UUID id = getUuidColumns(rs, 1);
			Instant created = getInstantColumn(rs, 3);

			ChargingScheduleInfo schedInfo = new ChargingScheduleInfo(
					UnitOfMeasure.forCode(rs.getInt(11)));
			schedInfo.setDurationSeconds(rs.getInt(9));
			schedInfo.setStart(getInstantColumn(rs, 10));
			schedInfo.setMinRate(rs.getBigDecimal(12));

			ChargingProfileInfo info = new ChargingProfileInfo(
					ChargingProfilePurpose.forCode(rs.getInt(4)),
					ChargingProfileKind.forCode(rs.getInt(5)), schedInfo);
			info.setRecurrency(ChargingScheduleRecurrency.forCode(rs.getInt(6)));
			info.setValidFrom(getInstantColumn(rs, 7));
			info.setValidTo(getInstantColumn(rs, 8));

			return new ChargingProfile(id, created, info);
		}

	}

	/**
	 * A result set extractor for profiles with associated periods.
	 */
	public static final class ChargingProfileResultSetExtractor
			implements ResultSetExtractor<List<ChargingProfile>> {

		private final RowMapper<ChargingProfile> mainRowMapper;
		private final RowMapper<ChargingSchedulePeriodInfo> periodRowMapper;

		public ChargingProfileResultSetExtractor() {
			super();
			mainRowMapper = new ChargingProfileRowMapper();
			periodRowMapper = new PeriodRowMapper(12);
		}

		@Override
		public List<ChargingProfile> extractData(ResultSet rs) throws SQLException, DataAccessException {
			List<ChargingProfile> result = new ArrayList<>(16);
			Map<UUID, ChargingProfile> map = new HashMap<>(16);
			int rowNum = 0;
			while ( rs.next() ) {
				rowNum++;
				ChargingProfile rowMainEntity = mainRowMapper.mapRow(rs, rowNum);
				ChargingProfile entity = map.get(rowMainEntity.getId());
				if ( entity == null ) {
					// new main entity row
					map.put(rowMainEntity.getId(), rowMainEntity);
					entity = rowMainEntity;
					result.add(entity);
				}
				ChargingSchedulePeriodInfo relRowEntity = periodRowMapper.mapRow(rs, rowNum);
				if ( relRowEntity != null ) {
					entity.getInfo().getSchedule().addPeriod(relRowEntity);
				}
			}
			return result;
		}

	}

}
