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
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
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

	private ChargePointIdentity createClientId() {
		return createClientId(UUID.randomUUID().toString());
	}

	private ChargePointIdentity createClientId(String identifier) {
		return new ChargePointIdentity(identifier, ChargePointIdentity.ANY_USER);
	}

	@Test
	public void auth_ok() {
		// given
		ChargePointIdentity identity = createClientId();
		Long id = UUID.randomUUID().getMostSignificantBits();
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		Authorization auth = new Authorization(id, Instant.now());
		auth.setToken(idTag);
		auth.setEnabled(true);
		expect(authorizationDao.getForToken(idTag)).andReturn(auth);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize(identity, idTag);

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
		ChargePointIdentity identity = createClientId();
		Long id = UUID.randomUUID().getMostSignificantBits();
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		Authorization auth = new Authorization(id, Instant.now());
		auth.setToken(idTag);
		auth.setEnabled(false);
		expect(authorizationDao.getForToken(idTag)).andReturn(auth);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize(identity, idTag);

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
		ChargePointIdentity identity = createClientId();
		Long id = UUID.randomUUID().getMostSignificantBits();
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		Authorization auth = new Authorization(id, Instant.now());
		auth.setToken(idTag);
		auth.setEnabled(true);
		auth.setExpiryDate(Instant.now().minusSeconds(60));
		expect(authorizationDao.getForToken(idTag)).andReturn(auth);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize(identity, idTag);

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
		ChargePointIdentity identity = createClientId();
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		expect(authorizationDao.getForToken(idTag)).andReturn(null);

		// when
		replayAll();
		AuthorizationInfo result = service.authorize(identity, idTag);

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
		String identifier = UUID.randomUUID().toString();
		ChargePointIdentity identity = createClientId(identifier);

		// look for existing charge point: not found
		expect(chargePointDao.getForIdentity(identity)).andReturn(null);

		// save new charge point
		Capture<ChargePoint> chargePointCaptor = new Capture<>(CaptureType.ALL);
		long chargePointId = UUID.randomUUID().getMostSignificantBits();
		expect(chargePointDao.save(capture(chargePointCaptor))).andReturn(chargePointId).times(2);

		// find broker for charge point, to send GetConfiguration message to
		expect(chargePointRouter.brokerForChargePoint(identity)).andReturn(chargePointBroker);

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
				ChargePoint copy = new ChargePoint(chargePointId, cp.getCreated(), cp.getInfo());
				copy.setEnabled(cp.isEnabled());
				copy.setRegistrationStatus(cp.getRegistrationStatus());
				copy.setConnectorCount(cp.getConnectorCount());
				return copy;
			}
		}).times(2);

		// look for existing charge point connectors
		expect(chargePointConnectorDao.findByChargePointId(chargePointId))
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
		info.setId(identifier);
		info.setChargePointVendor("ACME");
		info.setChargePointModel("One");
		ChargePoint result = service.registerChargePoint(identity, info);

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
		assertThat("Inserted charge point identifier preserved", inserted.getInfo().getId(),
				equalTo(identifier));
		assertThat("Inserted charge point connectors start at 0", inserted.getConnectorCount(),
				equalTo(0));

		ChargePoint updated = chargePointCaptor.getValues().get(1);
		assertThat("Updated charge point ID preserved", updated.getId(), equalTo(chargePointId));
		assertThat("Updated charge point identifier preserved", updated.getInfo().getId(),
				equalTo(identifier));
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
		String identifier = UUID.randomUUID().toString();
		ChargePointIdentity identity = createClientId(identifier);

		// look for existing charge point: not found
		ChargePointInfo cpInfo = new ChargePointInfo(identifier);
		cpInfo.setChargePointVendor("ACME");
		cpInfo.setChargePointModel("One");
		ChargePoint cp = new ChargePoint(UUID.randomUUID().getMostSignificantBits(), Instant.now(),
				cpInfo);
		cp.setConnectorCount(2);
		expect(chargePointDao.getForIdentity(identity)).andReturn(cp);

		// find broker for charge point, to send GetConfiguration message to
		expect(chargePointRouter.brokerForChargePoint(identity)).andReturn(chargePointBroker);

		// send GetConfiguration message to broker
		Capture<ActionMessage<Object>> actionCaptor = new Capture<>();
		Capture<ActionMessageResultHandler<Object, Object>> resultHandlerCaptor = new Capture<>();
		expect(chargePointBroker.sendMessageToChargePoint(capture(actionCaptor),
				capture(resultHandlerCaptor))).andReturn(true);

		// after response to get configuration, get ChargePoint again
		expect(chargePointDao.get(cp.getId())).andAnswer(new IAnswer<ChargePoint>() {

			@Override
			public ChargePoint answer() throws Throwable {
				return new ChargePoint(cp);
			}
		});

		// update connector count to 1
		Capture<ChargePoint> chargePointCaptor = new Capture<>();
		expect(chargePointDao.save(capture(chargePointCaptor))).andReturn(cp.getId());

		// look for existing charge point connectors
		List<ChargePointConnector> connectors = Arrays.asList(
				new ChargePointConnector(new ChargePointConnectorKey(cp.getId(), 1), Instant.now()),
				new ChargePointConnector(new ChargePointConnectorKey(cp.getId(), 2), Instant.now()));
		expect(chargePointConnectorDao.findByChargePointId(cp.getId())).andReturn(connectors);

		// remove extra connector
		int connectorCount = 1;
		Capture<ChargePointConnector> connectorCaptor = new Capture<>();
		chargePointConnectorDao.delete(capture(connectorCaptor));

		// when
		replayAll();
		ChargePointInfo info = new ChargePointInfo();
		info.setId(identifier);
		info.setChargePointVendor("ACME");
		info.setChargePointModel("One");
		ChargePoint result = service.registerChargePoint(identity, info);

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
