package com.lasthopesoftware.bluewater.client.connection.waking.specs.GivenAMachine;

import com.lasthopesoftware.bluewater.client.connection.waking.Machine;
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenSendingAWakeSignal {

	private static final byte[] expectedBytes = { -1, -1, -1, -1, -1, -1, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37 };

	private static byte[] sentBytes;

	@BeforeClass
	public static void before() throws MalformedURLException, ExecutionException, InterruptedException {
		final ServerAlarm serverAlarm = new ServerAlarm();
		sentBytes = new FuturePromise<>(serverAlarm.promiseWakeRequest(new Machine(
			new URL("http://my-sleeping-beauty"),
			"01-58-87-FA-91-25"))).get();
	}

	@Test
	public void thenTheSignalIsSent() {
		assertThat(sentBytes).containsExactly(expectedBytes);
	}
}
