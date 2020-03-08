package com.lasthopesoftware.bluewater.client.connection.waking.specs.GivenAMachine;

import com.lasthopesoftware.bluewater.client.connection.SendPackets;
import com.lasthopesoftware.bluewater.client.connection.waking.MachineAddress;
import com.lasthopesoftware.bluewater.client.connection.waking.ServerWakeSignal;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import kotlin.Unit;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenSendingAWakeSignal {

	private static final byte[] expectedBytes = { -1, -1, -1, -1, -1, -1, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37 };

	private static byte[] sentBytes;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final SendPackets connectionProvider = (host, port, bytes) -> {
			if (port != 9) return new Promise<>(Unit.INSTANCE);
			if (!host.equals("http://my-sleeping-beauty")) return new Promise<>(Unit.INSTANCE);
			sentBytes = bytes;
			return new Promise<>(Unit.INSTANCE);
		};

		final ServerWakeSignal serverWakeSignal = new ServerWakeSignal(connectionProvider);
		new FuturePromise<>(serverWakeSignal.promiseWakeSignal(
			new MachineAddress(
				"http://my-sleeping-beauty",
				"01-58-87-FA-91-25"),
			1,
			Duration.ZERO)).get();
	}

	@Test
	public void thenTheSignalIsCorrect() {
		assertThat(sentBytes).containsExactly(expectedBytes);
	}
}
