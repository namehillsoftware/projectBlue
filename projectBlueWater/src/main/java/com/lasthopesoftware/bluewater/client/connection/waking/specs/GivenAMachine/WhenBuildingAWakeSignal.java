package com.lasthopesoftware.bluewater.client.connection.waking.specs.GivenAMachine;

import com.lasthopesoftware.bluewater.client.connection.waking.Machine;
import com.lasthopesoftware.bluewater.client.connection.waking.WakeRequest;
import com.lasthopesoftware.bluewater.client.connection.waking.WakeRequestBuilder;

import org.junit.BeforeClass;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenBuildingAWakeSignal {

	private static final byte[] expectedBytes = { -1, -1, -1, -1, -1, -1, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37, 1, 88, -121, -6, -111, 37 };

	private static WakeRequest wakeRequest;

	@BeforeClass
	public static void before() throws MalformedURLException {
		final WakeRequestBuilder wakeRequestBuilder = new WakeRequestBuilder();
		wakeRequest = wakeRequestBuilder.buildWakeRequest(new Machine(
			new URL("http://my-sleeping-beauty"),
			"01-58-87-FA-91-25"));
	}

	@Test
	public void thenTheSignalIsCorrect() {
		assertThat(wakeRequest.getSignal()).containsExactly(expectedBytes);
	}

	@Test
	public void thenTheTargetMachineUrlIsCorrect() throws MalformedURLException {
		assertThat(wakeRequest.getTargetUrl()).isEqualTo(new URL("http://my-sleeping-beauty"));
	}
}
