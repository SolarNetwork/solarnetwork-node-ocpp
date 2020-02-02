/* ==================================================================
 * OcppWebSocketHandshakeInterceptor.java - 31/01/2020 4:19:08 pm
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

package net.solarnetwork.node.ocpp.v16.cs.json.web;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Intercept the OCPP Charge Point web socket handshake.
 * 
 * <p>
 * This interceptor will extract the Charge Point client ID from the request URI
 * and save that to the session attribute {@link #CLIENT_ID_ATTR}. If the client
 * ID is not available then a {@link HttpStatus#NOT_FOUND} error will be sent.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class OcppWebSocketHandshakeInterceptor implements HandshakeInterceptor {

	/** The attribute name for the {@link URI} of the HTTP request. */
	public static final String REQUEST_URI_ATTR = "requestUri";

	/** The default {@code clientIdUriPattern} property value. */
	public static final String DEFAULT_CLIENT_ID_URI_PATTERN = "/ocpp/v16/(.*)";

	/** The attribute key for the client ID. */
	public static final String CLIENT_ID_ATTR = "clientId";

	private static final Logger log = LoggerFactory.getLogger(OcppWebSocketHandshakeInterceptor.class);

	private Pattern clientIdUriPattern;

	/**
	 * Constructor.
	 */
	public OcppWebSocketHandshakeInterceptor() {
		super();
		setClientIdUriPattern(Pattern.compile(DEFAULT_CLIENT_ID_URI_PATTERN));
	}

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
		URI uri = request.getURI();
		Matcher m = clientIdUriPattern.matcher(uri.getPath());
		if ( !m.find() ) {
			log.debug("OCPP handshake request rejected, client ID not found in URI path: {}",
					uri.getPath());
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return false;
		}
		attributes.putIfAbsent(CLIENT_ID_ATTR, m.group(1));

		// enforce sub-protocol, as required by OCPP spec
		WebSocketHandler handler = WebSocketHandlerDecorator.unwrap(wsHandler);
		if ( handler instanceof SubProtocolCapable ) {
			List<String> subProtocols = ((SubProtocolCapable) handler).getSubProtocols();
			if ( subProtocols != null && !subProtocols.isEmpty() ) {
				WebSocketHttpHeaders headers = new WebSocketHttpHeaders(request.getHeaders());
				List<String> clientSubProtocols = headers.getSecWebSocketProtocol();
				boolean match = false;
				if ( clientSubProtocols != null ) {
					for ( String clientProtocol : clientSubProtocols ) {
						if ( subProtocols.contains(clientProtocol) ) {
							match = true;
							break;
						}
					}
				}
				if ( !match ) {
					log.debug(
							"OCPP handshake request rejected, supported sub-protocol(s) {}, requested: {}",
							subProtocols, clientSubProtocols);
					response.setStatusCode(HttpStatus.BAD_REQUEST);
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception) {
		// nothing to do
	}

	/**
	 * Get the Charge Point client ID URI pattern.
	 * 
	 * @return the pattern, never {@literal null}; defaults to
	 *         {@link #DEFAULT_CLIENT_ID_URI_PATTERN}
	 */
	public Pattern getClientIdUriPattern() {
		return clientIdUriPattern;
	}

	/**
	 * Set the Charge Point client ID URI pattern.
	 * 
	 * <p>
	 * This pattern is applied to the handshake request URI path, and should
	 * have a capturing group that returns the Charge Point client ID value.
	 * 
	 * @param clientIdUriPattern
	 */
	public void setClientIdUriPattern(Pattern clientIdUriPattern) {
		if ( clientIdUriPattern == null ) {
			throw new IllegalArgumentException("The client ID URI pattern must be provided.");
		}
		this.clientIdUriPattern = clientIdUriPattern;
	}

}
