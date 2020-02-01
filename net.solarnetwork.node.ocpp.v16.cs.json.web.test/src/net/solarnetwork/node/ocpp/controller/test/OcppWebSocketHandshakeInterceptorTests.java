/* ==================================================================
 * OcppWebSocketHandshakeInterceptor.java - 31/01/2020 4:49:04 pm
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

package net.solarnetwork.node.ocpp.controller.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import net.solarnetwork.node.ocpp.controller.OcppWebSocketSubProtocol;
import net.solarnetwork.node.ocpp.controller.v16.OcppWebSocketHandshakeInterceptor;

/**
 * Test cases for the {@link OcppWebSocketHandshakeInterceptor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppWebSocketHandshakeInterceptorTests {

	public static interface WebSocketHandlerAndSubProtocolCapable
			extends WebSocketHandler, SubProtocolCapable {

	}

	private ServerHttpRequest req;
	private ServerHttpResponse res;
	private WebSocketHandlerAndSubProtocolCapable handler;

	@Before
	public void setup() {
		req = EasyMock.createMock(ServerHttpRequest.class);
		res = EasyMock.createMock(ServerHttpResponse.class);
		handler = EasyMock.createMock(WebSocketHandlerAndSubProtocolCapable.class);
	}

	@After
	public void teardown() {
		EasyMock.verify(req, res, handler);
	}

	private void replayAll() {
		EasyMock.replay(req, res, handler);
	}

	@Test
	public void missingClientId() throws Exception {
		// given
		URI uri = URI.create("http://example.com/ocpp/v16");
		expect(req.getURI()).andReturn(uri);
		res.setStatusCode(HttpStatus.NOT_FOUND);

		OcppWebSocketHandshakeInterceptor hi = new OcppWebSocketHandshakeInterceptor();

		// when
		replayAll();
		Map<String, Object> attributes = new LinkedHashMap<>(4);
		boolean result = hi.beforeHandshake(req, res, handler, attributes);

		assertThat("Result failed from lack of client ID", result, equalTo(false));
		assertThat("No attributes populated", attributes.keySet(), hasSize(0));
	}

	@Test
	public void ok() throws Exception {
		// given
		URI uri = URI.create("http://example.com/ocpp/v16/foobar");
		expect(req.getURI()).andReturn(uri);
		expect(handler.getSubProtocols())
				.andReturn(Collections.singletonList(OcppWebSocketSubProtocol.OCPP_V16.getValue()));

		HttpHeaders h = new HttpHeaders();
		h.add(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL, OcppWebSocketSubProtocol.OCPP_V16.getValue());
		expect(req.getHeaders()).andReturn(h).anyTimes();

		OcppWebSocketHandshakeInterceptor hi = new OcppWebSocketHandshakeInterceptor();

		// when
		replayAll();
		Map<String, Object> attributes = new LinkedHashMap<>(4);
		boolean result = hi.beforeHandshake(req, res, handler, attributes);

		assertThat("Result success", result, equalTo(true));
		assertThat("Client ID attribute populated", attributes,
				hasEntry(OcppWebSocketHandshakeInterceptor.CLIENT_ID_ATTR, "foobar"));
	}

	@Test
	public void noSubProtocol() throws Exception {
		// given
		URI uri = URI.create("http://example.com/ocpp/v16/foobar");
		expect(req.getURI()).andReturn(uri);
		expect(handler.getSubProtocols())
				.andReturn(Collections.singletonList(OcppWebSocketSubProtocol.OCPP_V16.getValue()));

		HttpHeaders h = new HttpHeaders();
		expect(req.getHeaders()).andReturn(h).anyTimes();

		res.setStatusCode(HttpStatus.BAD_REQUEST);

		OcppWebSocketHandshakeInterceptor hi = new OcppWebSocketHandshakeInterceptor();

		// when
		replayAll();
		Map<String, Object> attributes = new LinkedHashMap<>(4);
		boolean result = hi.beforeHandshake(req, res, handler, attributes);

		assertThat("Result failed from missing sub-protocol", result, equalTo(false));
		assertThat("Client ID attribute populated", attributes,
				hasEntry(OcppWebSocketHandshakeInterceptor.CLIENT_ID_ATTR, "foobar"));
	}

	@Test
	public void wrongSubProtocol() throws Exception {
		// given
		URI uri = URI.create("http://example.com/ocpp/v16/foobar");
		expect(req.getURI()).andReturn(uri);
		expect(handler.getSubProtocols())
				.andReturn(Collections.singletonList(OcppWebSocketSubProtocol.OCPP_V16.getValue()));

		HttpHeaders h = new HttpHeaders();
		h.add(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL, OcppWebSocketSubProtocol.OCPP_V15.getValue());
		expect(req.getHeaders()).andReturn(h).anyTimes();

		res.setStatusCode(HttpStatus.BAD_REQUEST);

		OcppWebSocketHandshakeInterceptor hi = new OcppWebSocketHandshakeInterceptor();

		// when
		replayAll();
		Map<String, Object> attributes = new LinkedHashMap<>(4);
		boolean result = hi.beforeHandshake(req, res, handler, attributes);

		assertThat("Result failed from missing sub-protocol", result, equalTo(false));
		assertThat("Client ID attribute populated", attributes,
				hasEntry(OcppWebSocketHandshakeInterceptor.CLIENT_ID_ATTR, "foobar"));
	}
}
