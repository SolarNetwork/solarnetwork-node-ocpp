/* ==================================================================
 * OcppControllerServiceTests.java - 14/02/2020 10:58:35 am
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

package net.solarnetwork.node.ocpp.v16.cs.controller.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.ocpp.v16.cs.controller.OcppControllerService;
import net.solarnetwork.ocpp.dao.AuthorizationDao;
import net.solarnetwork.ocpp.dao.ChargePointConnectorDao;
import net.solarnetwork.ocpp.dao.ChargePointDao;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.domain.Authorization;
import net.solarnetwork.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointConnector;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.ocpp.service.ChargePointBroker;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import net.solarnetwork.test.CallingThreadExecutorService;
import ocpp.v16.ConfigurationKey;
import ocpp.v16.cp.GetConfigurationRequest;
import ocpp.v16.cp.GetConfigurationResponse;
import ocpp.v16.cp.KeyValue;

/**
 * Test cases for the {@link OcppControllerService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppControllerServiceTests {

	private final Executor executor = new CallingThreadExecutorService();
	private ChargePointRouter chargePointRouter;
	private ChargePointBroker chargePointBroker;
	private AuthorizationDao authorizationDao;
	private ChargePointDao chargePointDao;
	private ChargePointConnectorDao chargePointConnectorDao;
	private OcppControllerService service;

	@Before
	public void setup() {
		chargePointRouter = EasyMock.createMock(ChargePointRouter.class);
		chargePointBroker = EasyMock.createMock(ChargePointBroker.class);
		authorizationDao = EasyMock.createMock(AuthorizationDao.class);
		chargePointDao = EasyMock.createMock(ChargePointDao.class);
		chargePointConnectorDao = EasyMock.createMock(ChargePointConnectorDao.class);

		service = new OcppControllerService(executor, chargePointRouter, authorizationDao,
				chargePointDao, chargePointConnectorDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(chargePointRouter, chargePointBroker, authorizationDao, chargePointDao,
				chargePointConnectorDao);
	}

	private void replayAll() {
		EasyMock.replay(chargePointRouter, chargePointBroker, authorizationDao, chargePointDao,
				chargePointConnectorDao);
	}

	@Test
	public void auth_ok() {
		// given
		Long id = UUID.randomUUID().getMostSignificantBits();
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		Authorization auth = new Authorization(id, Instant.now());
		auth.setToken(idTag);
		auth.setEnabled(true);
		expect(authorizationDao.getForToken(idTag)).andReturn(auth);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize("foobar", idTag);

		// then
		assertThat("Result available", result, notNullValue());
		assertThat("Auth ID", result.getId(), equalTo(auth.getToken()));
		assertThat("Auth status", result.getStatus(), equalTo(AuthorizationStatus.Accepted));
		assertThat("Auth expiry", result.getExpiryDate(), equalTo(auth.getExpiryDate()));
		assertThat("Auth parent", result.getParentId(), equalTo(auth.getParentId()));
	}

	@Test
	public void auth_disabled() {
		// given
		Long id = UUID.randomUUID().getMostSignificantBits();
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		Authorization auth = new Authorization(id, Instant.now());
		auth.setToken(idTag);
		auth.setEnabled(false);
		expect(authorizationDao.getForToken(idTag)).andReturn(auth);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize("foobar", idTag);

		// then
		assertThat("Result available", result, notNullValue());
		assertThat("Auth ID", result.getId(), equalTo(auth.getToken()));
		assertThat("Auth status", result.getStatus(), equalTo(AuthorizationStatus.Blocked));
		assertThat("Auth expiry", result.getExpiryDate(), equalTo(auth.getExpiryDate()));
		assertThat("Auth parent", result.getParentId(), equalTo(auth.getParentId()));
	}

	@Test
	public void auth_expired() {
		// given
		Long id = UUID.randomUUID().getMostSignificantBits();
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		Authorization auth = new Authorization(id, Instant.now());
		auth.setToken(idTag);
		auth.setEnabled(true);
		auth.setExpiryDate(Instant.now().minusSeconds(60));
		expect(authorizationDao.getForToken(idTag)).andReturn(auth);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize("foobar", idTag);

		// then
		assertThat("Result available", result, notNullValue());
		assertThat("Auth ID", result.getId(), equalTo(auth.getToken()));
		assertThat("Auth status", result.getStatus(), equalTo(AuthorizationStatus.Expired));
		assertThat("Auth expiry", result.getExpiryDate(), equalTo(auth.getExpiryDate()));
		assertThat("Auth parent", result.getParentId(), equalTo(auth.getParentId()));
	}

	@Test
	public void auth_invalid() {
		// given
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		expect(authorizationDao.getForToken(idTag)).andReturn(null);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize("foobar", idTag);

		// then
		assertThat("Result available", result, notNullValue());
		assertThat("Auth ID", result.getId(), equalTo(idTag));
		assertThat("Auth status", result.getStatus(), equalTo(AuthorizationStatus.Invalid));
		assertThat("Auth expiry", result.getExpiryDate(), nullValue());
		assertThat("Auth parent", result.getParentId(), nullValue());
	}

	private static KeyValue conf(String key, String value) {
		return conf(key, value, false);
	}

	private static KeyValue conf(String key, String value, boolean readonly) {
		KeyValue kv = new KeyValue();
		kv.setKey(key);
		kv.setValue(value);
		kv.setReadonly(readonly);
		return kv;
	}

	@Test
	public void register_new() {
		// given
		String chargePointId = UUID.randomUUID().toString();

		// look for existing charge point: not found
		expect(chargePointDao.get(chargePointId)).andReturn(null);

		// save new charge point
		Capture<ChargePoint> chargePointCaptor = new Capture<>(CaptureType.ALL);
		expect(chargePointDao.save(capture(chargePointCaptor))).andReturn(chargePointId).times(2);

		// find broker for charge point, to send GetConfiguration message to
		expect(chargePointRouter.brokerForChargePoint(chargePointId)).andReturn(chargePointBroker);

		// send GetConfiguration message to broker
		Capture<ActionMessage<Object>> actionCaptor = new Capture<>();
		Capture<ActionMessageResultHandler<Object, Object>> resultHandlerCaptor = new Capture<>();
		expect(chargePointBroker.sendMessageToChargePoint(capture(actionCaptor),
				capture(resultHandlerCaptor))).andReturn(true);

		// after response to get configuration, get ChargePoint again
		expect(chargePointDao.get(chargePointId)).andAnswer(new IAnswer<ChargePoint>() {

			@Override
			public ChargePoint answer() throws Throwable {
				ChargePoint cp = chargePointCaptor.getValues().get(0);
				return new ChargePoint(cp);
			}
		});

		// look for existing charge point connectors
		expect(chargePointConnectorDao.findByIdChargePointId(chargePointId))
				.andReturn(Collections.emptyList());

		// insert new connector
		int connectorCount = 2;
		Capture<ChargePointConnector> connectorCaptor = new Capture<>(CaptureType.ALL);
		expect(chargePointConnectorDao.save(capture(connectorCaptor)))
				.andReturn(new ChargePointConnectorKey(chargePointId, 1));
		expect(chargePointConnectorDao.save(capture(connectorCaptor)))
				.andReturn(new ChargePointConnectorKey(chargePointId, 2));

		// when
		replayAll();
		ChargePointInfo info = new ChargePointInfo();
		info.setId(chargePointId);
		info.setChargePointVendor("ACME");
		info.setChargePointModel("One");
		ChargePoint result = service.registerChargePoint(info);

		// then invoke result handler
		ActionMessage<Object> message = actionCaptor.getValue();
		assertThat("Message sent to charge point is GetConfiguration", message.getMessage(),
				instanceOf(GetConfigurationRequest.class));
		ActionMessageResultHandler<Object, Object> resultHandler = resultHandlerCaptor.getValue();
		GetConfigurationResponse getConfRes = new GetConfigurationResponse();
		getConfRes.getConfigurationKey().add(
				conf(ConfigurationKey.NumberOfConnectors.getName(), String.valueOf(connectorCount)));
		boolean handlerResult = resultHandler.handleActionMessageResult(message, getConfRes, null);
		assertThat("Result handled", handlerResult, equalTo(true));

		// then
		assertThat("Result returned", result, notNullValue());

		ChargePoint inserted = chargePointCaptor.getValues().get(0);
		assertThat("Inserted charge point ID preserved", inserted.getId(), equalTo(chargePointId));
		assertThat("Inserted charge point connectors start at 0", inserted.getConnectorCount(),
				equalTo(0));

		ChargePoint updated = chargePointCaptor.getValues().get(1);
		assertThat("Updated charge point ID preserved", updated.getId(), equalTo(chargePointId));
		assertThat("Updated charge point connectors updated based on GetConfiguration response",
				updated.getConnectorCount(), equalTo(connectorCount));

		for ( int i = 0; i < connectorCount; i++ ) {
			ChargePointConnector conn = connectorCaptor.getValues().get(i);
			assertThat("Connector ID charge point ID " + i, conn.getId().getChargePointId(),
					equalTo(chargePointId));
			assertThat("Connector ID index " + i, conn.getId().getConnectorId(), equalTo(i + 1));
			assertThat("Connector info ID matches index " + i, conn.getInfo().getConnectorId(),
					equalTo(i + 1));
			assertThat("Connector info timestamp not null " + i, conn.getInfo().getTimestamp(),
					notNullValue());
		}
	}

	@Test
	public void register_decreaseConnectors() {
		// given
		String chargePointId = UUID.randomUUID().toString();

		// look for existing charge point: not found
		ChargePoint cp = new ChargePoint(chargePointId, Instant.now());
		ChargePointInfo cpInfo = new ChargePointInfo(chargePointId);
		cpInfo.setChargePointVendor("ACME");
		cpInfo.setChargePointModel("One");
		cp.setInfo(cpInfo);
		cp.setConnectorCount(2);
		expect(chargePointDao.get(chargePointId)).andReturn(cp);

		// find broker for charge point, to send GetConfiguration message to
		expect(chargePointRouter.brokerForChargePoint(chargePointId)).andReturn(chargePointBroker);

		// send GetConfiguration message to broker
		Capture<ActionMessage<Object>> actionCaptor = new Capture<>();
		Capture<ActionMessageResultHandler<Object, Object>> resultHandlerCaptor = new Capture<>();
		expect(chargePointBroker.sendMessageToChargePoint(capture(actionCaptor),
				capture(resultHandlerCaptor))).andReturn(true);

		// after response to get configuration, get ChargePoint again
		expect(chargePointDao.get(chargePointId)).andAnswer(new IAnswer<ChargePoint>() {

			@Override
			public ChargePoint answer() throws Throwable {
				return new ChargePoint(cp);
			}
		});

		// udpate connector count to 1
		Capture<ChargePoint> chargePointCaptor = new Capture<>();
		expect(chargePointDao.save(capture(chargePointCaptor))).andReturn(chargePointId);

		// look for existing charge point connectors
		List<ChargePointConnector> connectors = Arrays.asList(
				new ChargePointConnector(new ChargePointConnectorKey(chargePointId, 1), Instant.now()),
				new ChargePointConnector(new ChargePointConnectorKey(chargePointId, 2), Instant.now()));
		expect(chargePointConnectorDao.findByIdChargePointId(chargePointId)).andReturn(connectors);

		// remove extra connector
		int connectorCount = 1;
		Capture<ChargePointConnector> connectorCaptor = new Capture<>();
		chargePointConnectorDao.delete(capture(connectorCaptor));

		// when
		replayAll();
		ChargePointInfo info = new ChargePointInfo();
		info.setId(chargePointId);
		info.setChargePointVendor("ACME");
		info.setChargePointModel("One");
		ChargePoint result = service.registerChargePoint(info);

		// then invoke result handler
		ActionMessage<Object> message = actionCaptor.getValue();
		assertThat("Message sent to charge point is GetConfiguration", message.getMessage(),
				instanceOf(GetConfigurationRequest.class));
		ActionMessageResultHandler<Object, Object> resultHandler = resultHandlerCaptor.getValue();
		GetConfigurationResponse getConfRes = new GetConfigurationResponse();
		getConfRes.getConfigurationKey().add(
				conf(ConfigurationKey.NumberOfConnectors.getName(), String.valueOf(connectorCount)));
		boolean handlerResult = resultHandler.handleActionMessageResult(message, getConfRes, null);
		assertThat("Result handled", handlerResult, equalTo(true));

		// then
		assertThat("Result returned", result, notNullValue());

		assertThat("Connector count updated", chargePointCaptor.getValue().getConnectorCount(),
				equalTo(connectorCount));

		ChargePointConnector conn = connectorCaptor.getValue();
		assertThat("Deleted extra connector", conn, equalTo(connectors.get(1)));
	}
}
