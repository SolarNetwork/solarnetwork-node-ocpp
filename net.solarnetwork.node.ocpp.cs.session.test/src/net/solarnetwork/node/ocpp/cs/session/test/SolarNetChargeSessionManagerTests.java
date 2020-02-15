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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.ocpp.cs.session.SolarNetChargeSessionManager;
import net.solarnetwork.node.ocpp.dao.ChargeSessionDao;
import net.solarnetwork.node.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.node.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.node.ocpp.domain.ChargeSession;
import net.solarnetwork.node.ocpp.domain.ChargeSessionStartInfo;
import net.solarnetwork.node.ocpp.domain.Location;
import net.solarnetwork.node.ocpp.domain.Measurand;
import net.solarnetwork.node.ocpp.domain.ReadingContext;
import net.solarnetwork.node.ocpp.domain.SampledValue;
import net.solarnetwork.node.ocpp.domain.UnitOfMeasure;
import net.solarnetwork.node.ocpp.service.AuthorizationService;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link SolarNetChargeSessionManager} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SolarNetChargeSessionManagerTests {

	private AuthorizationService authService;
	private ChargeSessionDao chargeSessionDao;
	private DatumDao<GeneralNodeDatum> datumDao;
	private SolarNetChargeSessionManager manager;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		authService = EasyMock.createMock(AuthorizationService.class);
		chargeSessionDao = EasyMock.createMock(ChargeSessionDao.class);
		datumDao = EasyMock.createMock(DatumDao.class);
		manager = new SolarNetChargeSessionManager(authService, chargeSessionDao,
				new StaticOptionalService<>(datumDao));
	}

	@After
	public void teardown() {
		EasyMock.verify(authService, chargeSessionDao, datumDao);
	}

	private void replayAll() {
		EasyMock.replay(authService, chargeSessionDao, datumDao);
	}

	@Test
	public void startSession_ok() {
		// given

		// verify authorization
		String chargePointId = UUID.randomUUID().toString();
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		AuthorizationInfo authInfo = new AuthorizationInfo(idTag, AuthorizationStatus.Accepted);
		expect(authService.authorize(chargePointId, idTag)).andReturn(authInfo);

		// verify concurrent tx
		int connectorId = 1;
		expect(chargeSessionDao.getIncompleteChargeSessionForConnector(chargePointId, connectorId))
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
				sessionCaptor.getValue().getChargePointId(), equalTo(info.getChargePointId()));
		assertThat("Stored session auth ID matches request", sessionCaptor.getValue().getAuthId(),
				equalTo(info.getAuthorizationId()));
		assertThat("Stored session connector ID matches request",
				sessionCaptor.getValue().getConnectorId(), equalTo(info.getConnectorId()));

		assertThat("Created session ID matches refresh ID request", sessionIdCaptor.getValue(),
				equalTo(sessionCaptor.getValue().getId()));
		assertThat("Charge Point ID returned", sess.getChargePointId(), equalTo(chargePointId));
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
		assertThat("Datum source ID", datum.getSourceId(), equalTo(
				String.format("/ocpp/cp/%s/%d/%s", chargePointId, connectorId, Location.Outlet)));
		assertThat("Energy prop", datum.getAccumulatingSampleLong(ACEnergyDatum.WATT_HOUR_READING_KEY),
				equalTo(info.getMeterStart()));
		assertThat("Datum prop session ID",
				datum.getStatusSampleString(SolarNetChargeSessionManager.SESSION_ID_PROPERTY),
				equalTo(sess.getId().toString()));
	}

}
