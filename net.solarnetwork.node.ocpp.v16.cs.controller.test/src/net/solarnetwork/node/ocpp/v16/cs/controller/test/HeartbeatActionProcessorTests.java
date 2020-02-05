/* ==================================================================
 * HeartbeatActionProcessorTests.java - 5/02/2020 2:57:02 pm
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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import net.solarnetwork.node.ocpp.domain.BasicActionMessage;
import net.solarnetwork.node.ocpp.v16.cs.controller.HeartbeatActionProcessor;
import ocpp.v16.CentralSystemAction;
import ocpp.v16.cs.HeartbeatRequest;

/**
 * Test cases for the {@link HeartbeatActionProcessor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class HeartbeatActionProcessorTests {

	@Test
	public void ok() throws InterruptedException {
		// given
		final HeartbeatActionProcessor p = new HeartbeatActionProcessor();
		final CountDownLatch l = new CountDownLatch(1);

		// when
		HeartbeatRequest req = new HeartbeatRequest();
		BasicActionMessage<HeartbeatRequest> act = new BasicActionMessage<>(
				CentralSystemAction.Heartbeat, req);
		p.processActionMessage(act, (msg, res, err) -> {
			assertThat("Message preserved", msg, sameInstance(act));
			assertThat("Error not present", err, nullValue());
			assertThat("Response provided", res, notNullValue());
			final long now = System.currentTimeMillis();
			assertThat("Response currentTime close to now",
					now - res.getCurrentTime().toGregorianCalendar().getTimeInMillis(),
					allOf(greaterThanOrEqualTo(0L), lessThan(100L)));
			l.countDown();
			return true;
		});

		// then
		assertThat("Result handler invoked", l.await(1, TimeUnit.SECONDS), equalTo(true));
	}

	@Test
	public void nullRequest() throws InterruptedException {
		// given
		final HeartbeatActionProcessor p = new HeartbeatActionProcessor();
		final CountDownLatch l = new CountDownLatch(1);

		// when
		HeartbeatRequest req = new HeartbeatRequest();
		BasicActionMessage<HeartbeatRequest> act = new BasicActionMessage<>(
				CentralSystemAction.Heartbeat, req);
		p.processActionMessage(act, (msg, res, err) -> {
			assertThat("Message preserved", msg, sameInstance(act));
			assertThat("Error not present", err, nullValue());
			assertThat("Response provided", res, notNullValue());
			final long now = System.currentTimeMillis();
			assertThat("Response currentTime close to now",
					now - res.getCurrentTime().toGregorianCalendar().getTimeInMillis(),
					allOf(greaterThanOrEqualTo(0L), lessThan(100L)));
			l.countDown();
			return true;
		});

		// then
		assertThat("Result handler invoked", l.await(1, TimeUnit.SECONDS), equalTo(true));
	}

	@Test(expected = NullPointerException.class)
	public void nullHandler() {
		new HeartbeatActionProcessor().processActionMessage(null, null);
	}

}
