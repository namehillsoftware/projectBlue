package com.lasthopesoftware.bluewater.client.connection.waking.specs.GivenAMachine;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.waking.Machine;
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import kotlin.Unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenBuildingAWakeSignal {

	private static final byte[] expectedBytes = { -1, -1, -1, -1, -1, -1, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37 };

	private static byte[] sentBytes;

	@BeforeClass
	public static void before() throws MalformedURLException, ExecutionException, InterruptedException {
		final IConnectionProvider connectionProvider = mock(IConnectionProvider.class);
		when(connectionProvider.promiseSentPacket(any()))
			.thenAnswer(a -> {
				sentBytes = a.getArgument(0);
				return new Promise<>(Unit.INSTANCE);
			});

		final ServerAlarm serverAlarm = new ServerAlarm(connectionProvider);
		new FuturePromise<>(serverAlarm.promiseWakeRequest(new Machine(
			new URL("http://my-sleeping-beauty"),
			"01-58-87-FA-91-25"))).get();
	}

	@Test
	public void thenTheSignalIsCorrect() {
		assertThat(sentBytes).containsExactly(expectedBytes);
	}
}
