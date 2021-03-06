/* ==================================================================
 * SolarNetChargeSessionManagerTests.java - 15/02/2020 12:38:14 pm
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

package net.solarnetwork.node.ocpp.cs.session.test;

import static java.util.Arrays.asList;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.node.PlaceholderService;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.ocpp.cs.session.SolarNetChargeSessionManager;
import net.solarnetwork.ocpp.dao.ChargePointDao;
import net.solarnetwork.ocpp.dao.ChargeSessionDao;
import net.solarnetwork.ocpp.dao.PurgePostedChargeSessionsTask;
import net.solarnetwork.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.ocpp.domain.ChargeSessionEndInfo;
import net.solarnetwork.ocpp.domain.ChargeSessionEndReason;
import net.solarnetwork.ocpp.domain.ChargeSessionStartInfo;
import net.solarnetwork.ocpp.domain.Location;
import net.solarnetwork.ocpp.domain.Measurand;
import net.solarnetwork.ocpp.domain.ReadingContext;
import net.solarnetwork.ocpp.domain.SampledValue;
import net.solarnetwork.ocpp.domain.UnitOfMeasure;
import net.solarnetwork.ocpp.service.AuthorizationException;
import net.solarnetwork.ocpp.service.AuthorizationService;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link SolarNetChargeSessionManager} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SolarNetChargeSessionManagerTests {

	private AuthorizationService authService;
	private ChargePointDao chargePointDao;
	private ChargeSessionDao chargeSessionDao;
	private DatumDao<GeneralNodeDatum> datumDao;
	private TaskScheduler taskScheduler;
	private PlaceholderService placeholderService;
	private SolarNetChargeSessionManager manager;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		authService = createMock(AuthorizationService.class);
		chargePointDao = createMock(ChargePointDao.class);
		chargeSessionDao = createMock(ChargeSessionDao.class);
		datumDao = createMock(DatumDao.class);
		taskScheduler = createMock(TaskScheduler.class);
		placeholderService = createMock(PlaceholderService.class);
		manager = new SolarNetChargeSessionManager(authService, chargePointDao, chargeSessionDao,
				new StaticOptionalService<>(datumDao));
		manager.setTaskScheduler(taskScheduler);
	}

	@After
	public void teardown() {
		EasyMock.verify(authService, chargePointDao, chargeSessionDao, datumDao, taskScheduler,
				placeholderService);
	}

	private void replayAll(Object... mocks) {
		EasyMock.replay(authService, chargePointDao, chargeSessionDao, datumDao, taskScheduler,
				placeholderService);
		if ( mocks != null ) {
			EasyMock.replay(mocks);
		}
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void startup() {
		// given
		int expireHours = 2;
		manager.setPurgePostedChargeSessionsExpirationHours(expireHours);

		Capture<Runnable> startupTaskCaptor = new Capture<>();
		ScheduledFuture<Object> startupTaskFuture = createMock(ScheduledFuture.class);
		expect(taskScheduler.schedule(capture(startupTaskCaptor), anyObject(Date.class)))
				.andReturn((ScheduledFuture) startupTaskFuture);

		long taskDelay = TimeUnit.HOURS.toMillis(expireHours) / 4;
		ScheduledFuture<Object> purgePostedTaskFuture = createMock(ScheduledFuture.class);
		Capture<Runnable> purgeTaskCaptor = new Capture<>();
		expect(taskScheduler.scheduleWithFixedDelay(capture(purgeTaskCaptor), anyObject(),
				eq(taskDelay))).andReturn((ScheduledFuture) purgePostedTaskFuture);

		// when
		replayAll(startupTaskFuture, purgePostedTaskFuture);

		manager.startup();

		Runnable startupTask = startupTaskCaptor.getValue();
		startupTask.run();

		// then
		assertThat("Purge posted task scheduled", purgeTaskCaptor.getValue(),
				instanceOf(PurgePostedChargeSessionsTask.class));
		verify(startupTaskFuture, purgePostedTaskFuture);
	}

	@Test
	public void startSession_ok() {
		// given

		// verify authorization
		String identifier = UUID.randomUUID().toString();
		ChargePointIdentity chargePointId = new ChargePointIdentity(identifier, "foo");
		ChargePoint cp = new ChargePoint(UUID.randomUUID().getMostSignificantBits(), Instant.now(),
				new ChargePointInfo(identifier));
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		AuthorizationInfo authInfo = new AuthorizationInfo(idTag, AuthorizationStatus.Accepted);
		expect(authService.authorize(chargePointId, idTag)).andReturn(authInfo);

		// get ChargePoint
		expect(chargePointDao.getForIdentity(chargePointId)).andReturn(cp);

		// verify concurrent tx
		int connectorId = 1;
		expect(chargeSessionDao.getIncompleteChargeSessionForConnector(cp.getId(), connectorId))
				.andReturn(null);

		// create new session
		Capture<ChargeSession> sessionCaptor = new Capture<>();
		expect(chargeSessionDao.save(capture(sessionCaptor))).andAnswer(new IAnswer<UUID>() {

			@Override
			public UUID answer() throws Throwable {
				return sessionCaptor.getValue().getId();
			}
		});

		// refresh to get txid
		Capture<UUID> sessionIdCaptor = new Capture<>();
		int transactionId = 123;
		expect(chargeSessionDao.get(capture(sessionIdCaptor))).andAnswer(new IAnswer<ChargeSession>() {

			@Override
			public ChargeSession answer() throws Throwable {
				ChargeSession old = sessionCaptor.getValue();
				return new ChargeSession(old.getId(), old.getCreated(), old.getAuthId(),
						old.getChargePointId(), old.getConnectorId(), transactionId);
			}
		});

		// store initial reading
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		// generate datum from initial reading
		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		datumDao.storeDatum(capture(datumCaptor));

		// when
		replayAll();

		// @formatter:off
		ChargeSessionStartInfo info = ChargeSessionStartInfo.builder()
				.withTimestampStart(Instant.now())
				.withChargePointId(chargePointId)
				.withAuthorizationId(idTag)
				.withConnectorId(connectorId)
				.withMeterStart(1234)
				.build();
		// @formatter:on

		ChargeSession sess = manager.startChargingSession(info);

		// then
		assertThat("Session created", sess, notNullValue());

		assertThat("Stored session ID not null", sessionCaptor.getValue(), notNullValue());
		assertThat("Stored session timestamp ID matches request", sessionCaptor.getValue().getCreated(),
				equalTo(info.getTimestampStart()));
		assertThat("Stored session Charge Point ID matches request",
				sessionCaptor.getValue().getChargePointId(), equalTo(cp.getId()));
		assertThat("Stored session auth ID matches request", sessionCaptor.getValue().getAuthId(),
				equalTo(info.getAuthorizationId()));
		assertThat("Stored session connector ID matches request",
				sessionCaptor.getValue().getConnectorId(), equalTo(info.getConnectorId()));

		assertThat("Created session ID matches refresh ID request", sessionIdCaptor.getValue(),
				equalTo(sessionCaptor.getValue().getId()));
		assertThat("Charge Point ID returned", sess.getChargePointId(), equalTo(cp.getId()));
		assertThat("Auth ID returned", sess.getAuthId(), equalTo(idTag));
		assertThat("Connector ID returned", sess.getConnectorId(), equalTo(connectorId));
		assertThat("Transaction ID returned", sess.getTransactionId(), equalTo(transactionId));

		List<SampledValue> samples = StreamSupport.stream(readingsCaptor.getValue().spliterator(), false)
				.collect(Collectors.toList());

		// @formatter:off
		SampledValue expectedReading = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.build();
		// @formatter:on

		assertThat("One reading saved", samples, hasSize(1));
		assertThat("Initial reading core properties", samples.get(0), equalTo(expectedReading));
		assertThat("Initial reading unit", samples.get(0).getUnit(), equalTo(UnitOfMeasure.Wh));
		assertThat("Initial reading value", samples.get(0).getValue(),
				equalTo(String.valueOf(info.getMeterStart())));

		GeneralNodeDatum datum = datumCaptor.getValue();
		assertThat("Datum generated", datum, notNullValue());
		assertThat("Datum date", datum.getCreated().getTime(),
				equalTo(sess.getCreated().toEpochMilli()));
		assertThat("Datum source ID", datum.getSourceId(),
				equalTo(String.format("/ocpp/cp/%s/%d/%s", identifier, connectorId, Location.Outlet)));
		assertThat("Energy prop", datum.getAccumulatingSampleLong(ACEnergyDatum.WATT_HOUR_READING_KEY),
				equalTo(info.getMeterStart()));
		assertThat("Datum prop session ID",
				datum.getStatusSampleString(SolarNetChargeSessionManager.SESSION_ID_PROPERTY),
				equalTo(sess.getId().toString()));
	}

	@Test
	public void startSession_concurrentTx() {
		// given

		// verify authorization
		String identifier = UUID.randomUUID().toString();
		ChargePointIdentity chargePointId = new ChargePointIdentity(identifier, "foo");
		ChargePoint cp = new ChargePoint(UUID.randomUUID().getMostSignificantBits(), Instant.now(),
				new ChargePointInfo(identifier));
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		AuthorizationInfo authInfo = new AuthorizationInfo(idTag, AuthorizationStatus.Accepted);
		expect(authService.authorize(chargePointId, idTag)).andReturn(authInfo);

		// get ChargePoint
		expect(chargePointDao.getForIdentity(chargePointId)).andReturn(cp);

		// verify concurrent tx
		int connectorId = 1;
		int transactionId = 123;
		ChargeSession existingSess = new ChargeSession(UUID.randomUUID(), Instant.now().minusSeconds(60),
				idTag, cp.getId(), connectorId, transactionId);
		expect(chargeSessionDao.getIncompleteChargeSessionForConnector(cp.getId(), connectorId))
				.andReturn(existingSess);

		// when
		replayAll();

		// @formatter:off
		ChargeSessionStartInfo info = ChargeSessionStartInfo.builder()
				.withTimestampStart(Instant.now())
				.withChargePointId(chargePointId)
				.withAuthorizationId(idTag)
				.withConnectorId(connectorId)
				.withMeterStart(1234)
				.build();
		// @formatter:on

		try {
			manager.startChargingSession(info);
			fail("Should have failed with ConcurrentTx");
		} catch ( AuthorizationException e ) {
			assertThat("Authorization info available", e.getInfo(), notNullValue());
			assertThat("Authorization status is ConcurrentTx", e.getInfo().getStatus(),
					equalTo(AuthorizationStatus.ConcurrentTx));
		}
	}

	@Test
	public void endSession_ok() {
		// given
		String idTag = "tester";
		String identifier = UUID.randomUUID().toString();
		ChargePointIdentity chargePointId = new ChargePointIdentity(identifier, "foo");
		ChargePoint cp = new ChargePoint(UUID.randomUUID().getMostSignificantBits(), Instant.now(),
				new ChargePointInfo(identifier));
		int connectorId = 1;
		int transactionId = 123;

		// get ChargePoint
		expect(chargePointDao.getForIdentity(chargePointId)).andReturn(cp);

		ChargeSession sess = new ChargeSession(UUID.randomUUID(), Instant.now(), idTag, cp.getId(),
				connectorId, transactionId);
		expect(chargeSessionDao.getIncompleteChargeSessionForTransaction(cp.getId(), transactionId))
				.andReturn(sess);

		Capture<ChargeSession> updatedCaptor = new Capture<>();
		expect(chargeSessionDao.save(capture(updatedCaptor))).andReturn(sess.getId());

		// @formatter:off
		SampledValue startReading = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.build();
		SampledValue middleReading = SampledValue.builder()
				.withTimestamp(sess.getCreated().plusSeconds(10))
				.withSessionId(sess.getId())
				.withContext(ReadingContext.SamplePeriodic)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.build();
		// @formatter:on
		expect(chargeSessionDao.findReadingsForSession(sess.getId()))
				.andReturn(Arrays.asList(startReading, middleReading));

		Capture<Iterable<SampledValue>> newReadingsCapture = new Capture<>();
		chargeSessionDao.addReadings(capture(newReadingsCapture));

		// generate datum from initial reading
		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		datumDao.storeDatum(capture(datumCaptor));
		// when
		replayAll();

		// @formatter:off
		ChargeSessionEndInfo info = ChargeSessionEndInfo.builder()
				.withTimestampEnd(Instant.now())
				.withAuthorizationId(idTag)
				.withChargePointId(chargePointId)
				.withTransactionId(transactionId)
				.withMeterEnd(54321)
				.withReason(ChargeSessionEndReason.Local)
				.build();
		// @formatter:on
		manager.endChargingSession(info);

		// then

		// @formatter:off
		SampledValue endReading = SampledValue.builder()
				.withTimestamp(info.getTimestampEnd())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionEnd)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.build();
		// @formatter:on

		List<SampledValue> samples = StreamSupport
				.stream(newReadingsCapture.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat("One reading saved", samples, hasSize(1));
		assertThat("Initial reading core properties", samples.get(0), equalTo(endReading));
		assertThat("Initial reading unit", samples.get(0).getUnit(), equalTo(UnitOfMeasure.Wh));
		assertThat("Initial reading value", samples.get(0).getValue(),
				equalTo(String.valueOf(info.getMeterEnd())));

		// session should be update with end/posted dates
		ChargeSession updated = updatedCaptor.getValue();
		assertThat("Session ID same", updated.getId(), equalTo(sess.getId()));
		assertThat("End date set", updated.getEnded(), equalTo(info.getTimestampEnd()));
		assertThat("End auth ID set", updated.getAuthId(), equalTo(info.getAuthorizationId()));
		assertThat("Posted date set", updated.getPosted(), notNullValue());

		GeneralNodeDatum datum = datumCaptor.getValue();
		assertThat("Datum generated", datum, notNullValue());
		assertThat("Datum date", datum.getCreated().getTime(),
				equalTo(info.getTimestampEnd().toEpochMilli()));
		assertThat("Datum source ID", datum.getSourceId(),
				equalTo(String.format("/ocpp/cp/%s/%d/%s", identifier, connectorId, Location.Outlet)));
		assertThat("Energy prop", datum.getAccumulatingSampleLong(ACEnergyDatum.WATT_HOUR_READING_KEY),
				equalTo(info.getMeterEnd()));
		assertThat("Datum prop session ID",
				datum.getStatusSampleString(SolarNetChargeSessionManager.SESSION_ID_PROPERTY),
				equalTo(sess.getId().toString()));
	}

	@Test
	public void addReadings_consolidate() {
		// given
		String idTag = "tester";
		String identifier = UUID.randomUUID().toString();
		ChargePoint cp = new ChargePoint(UUID.randomUUID().getMostSignificantBits(), Instant.now(),
				new ChargePointInfo(identifier));
		int connectorId = 1;
		int transactionId = 123;

		// get ChargePoint
		expect(chargePointDao.get(cp.getId())).andReturn(cp);

		// get current session
		ChargeSession sess = new ChargeSession(UUID.randomUUID(), Instant.now(), idTag, cp.getId(),
				connectorId, transactionId);
		expect(chargeSessionDao.get(sess.getId())).andReturn(sess);

		// get current readings for session
		expect(chargeSessionDao.findReadingsForSession(sess.getId())).andReturn(Collections.emptyList());

		// save readings
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>(CaptureType.ALL);
		datumDao.storeDatum(capture(datumCaptor));
		expectLastCall().times(3);

		// when
		replayAll();

		// @formatter:off
		SampledValue r1 = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("1234")
				.build();
		SampledValue r2 = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.PowerActiveImport)
				.withUnit(UnitOfMeasure.W)
				.withValue("500")
				.build();
		SampledValue r3 = SampledValue.builder()
				.withTimestamp(sess.getCreated().plusSeconds(60))
				.withSessionId(sess.getId())
				.withContext(ReadingContext.SamplePeriodic)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("2345")
				.build();
		SampledValue r4 = SampledValue.builder()
				.withTimestamp(sess.getCreated().plusSeconds(60))
				.withSessionId(sess.getId())
				.withContext(ReadingContext.SamplePeriodic)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.PowerActiveImport)
				.withUnit(UnitOfMeasure.W)
				.withValue("400")
				.build();
		SampledValue r5 = SampledValue.builder()
				.withTimestamp(sess.getCreated().plusSeconds(60))
				.withSessionId(sess.getId())
				.withContext(ReadingContext.SamplePeriodic)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.Frequency)
				.withValue("59.89")
				.build();
		SampledValue r6 = SampledValue.builder()
				.withTimestamp(sess.getCreated().plusSeconds(90))
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionEnd)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withValue("3456")
				.build();
		// @formatter:on
		manager.addChargingSessionReadings(asList(r1, r2, r3, r4, r5, r6));

		// then
		assertThat("Persisted readings same as passed in", readingsCaptor.getValue(),
				contains(r1, r2, r3, r4, r5, r6));

		List<GeneralNodeDatum> persistedDatum = datumCaptor.getValues();
		assertThat("Consolidated readings into 3 datum based on date", persistedDatum, hasSize(3));

		for ( int i = 0; i < persistedDatum.size(); i++ ) {
			GeneralNodeDatum d = persistedDatum.get(i);
			assertThat("Datum source ID " + i, d.getSourceId(),
					equalTo("/ocpp/cp/" + identifier + "/" + connectorId + "/Outlet"));
			assertThat("Datum session ID " + i, d.getStatusSampleString("sessionId"),
					equalTo(sess.getId().toString()));
		}

		GeneralNodeDatum d = persistedDatum.get(0);
		assertThat("Datum 1 @ transaction start", d.getCreated().getTime(),
				equalTo(r1.getTimestamp().toEpochMilli()));
		assertThat("Datum 1 consolidated properties", d.getSampleData(),
				allOf(hasEntry("wattHours", new BigDecimal(r1.getValue())),
						hasEntry("watts", new BigDecimal(r2.getValue()))));

		d = persistedDatum.get(1);
		assertThat("Datum 2 @ middle", d.getCreated().getTime(),
				equalTo(r3.getTimestamp().toEpochMilli()));
		assertThat("Datum 2 consolidated properties", d.getSampleData(),
				allOf(hasEntry("wattHours", new BigDecimal(r3.getValue())),
						hasEntry("watts", new BigDecimal(r4.getValue())),
						hasEntry("frequency", new BigDecimal(r5.getValue()))));

		d = persistedDatum.get(2);
		assertThat("Datum 2 @ transaction end", d.getCreated().getTime(),
				equalTo(r6.getTimestamp().toEpochMilli()));
		assertThat("Datum 2 consolidated properties", d.getSampleData(),
				hasEntry("wattHours", new BigDecimal(r6.getValue())));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void addReading_withPlaceholderService() {
		// GIVEN
		manager.setPlaceholderService(new StaticOptionalService<>(placeholderService));

		String idTag = "tester";
		String identifier = UUID.randomUUID().toString();
		ChargePoint cp = new ChargePoint(UUID.randomUUID().getMostSignificantBits(), Instant.now(),
				new ChargePointInfo(identifier));
		int connectorId = 1;
		int transactionId = 123;

		// get ChargePoint
		expect(chargePointDao.get(cp.getId())).andReturn(cp);

		// get current session
		ChargeSession sess = new ChargeSession(UUID.randomUUID(), Instant.now(), idTag, cp.getId(),
				connectorId, transactionId);
		expect(chargeSessionDao.get(sess.getId())).andReturn(sess);

		// get current readings for session
		expect(chargeSessionDao.findReadingsForSession(sess.getId())).andReturn(Collections.emptyList());

		// save readings
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		expect(placeholderService.resolvePlaceholders(
				eq(SolarNetChargeSessionManager.DEFAULT_SOURCE_ID_TEMPLATE), anyObject(Map.class)))
						.andReturn("RESOLVED");

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>(CaptureType.ALL);
		datumDao.storeDatum(capture(datumCaptor));
		expectLastCall().times(1);

		// when
		replayAll();

		// @formatter:off
		SampledValue r1 = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("1234")
				.build();
		// @formatter:on
		manager.addChargingSessionReadings(asList(r1));

		// then
		assertThat("Persisted readings same as passed in", readingsCaptor.getValue(), contains(r1));

		List<GeneralNodeDatum> persistedDatum = datumCaptor.getValues();
		assertThat("Readings into datum based on date", persistedDatum, hasSize(1));

		for ( int i = 0; i < persistedDatum.size(); i++ ) {
			GeneralNodeDatum d = persistedDatum.get(i);
			//assertThat("Datum source ID " + i, d.getSourceId(), equalTo("RESOLVED"));
			assertThat("Datum session ID " + i, d.getStatusSampleString("sessionId"),
					equalTo(sess.getId().toString()));
		}

		GeneralNodeDatum d = persistedDatum.get(0);
		assertThat("Datum 1 @ transaction start", d.getCreated().getTime(),
				equalTo(r1.getTimestamp().toEpochMilli()));
		assertThat("Datum 1 properties", d.getSampleData(),
				hasEntry("wattHours", new BigDecimal(r1.getValue())));
	}

}
