package com.lasthopesoftware.bluewater.client.servers.version.specs.GivenAConnectionProviderThatDoesNotReturnProgramVersion;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersionProvider;
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenReceivingThePromisedProgramVersion {

	private static SemanticVersion version;

	@BeforeClass
	public static void before() throws InterruptedException {
		final FakeConnectionProvider connectionProvider = new FakeConnectionProvider();

		connectionProvider.mapResponse((p) -> new FakeConnectionResponseTuple(200, "<Response Status=\"OK\"></Response>".getBytes()), "Alive");

		final ProgramVersionProvider programVersionProvider = new ProgramVersionProvider(connectionProvider);
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		programVersionProvider.promiseServerVersion().then(v -> {
			version = v;
			countDownLatch.countDown();
			return null;
		});

		countDownLatch.await();
	}

	@Test
	public void thenTheServerVersionIsNull() {
		assertThat(version).isNull();
	}
}
