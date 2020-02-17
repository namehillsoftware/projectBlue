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

	private static Boolean wasSent;

	@BeforeClass
	public static void before() throws MalformedURLException, ExecutionException, InterruptedException {
		final ServerAlarm serverAlarm = new ServerAlarm();
		wasSent = new FuturePromise<>(serverAlarm.promiseWakeRequest(new Machine(
			new URL("http://my-sleeping-beauty"),
			"01-58-87-HA-91-25"))).get();
	}

	@Test
	public void thenTheSignalIsSent() {
		assertThat(wasSent).isTrue();
	}
}
