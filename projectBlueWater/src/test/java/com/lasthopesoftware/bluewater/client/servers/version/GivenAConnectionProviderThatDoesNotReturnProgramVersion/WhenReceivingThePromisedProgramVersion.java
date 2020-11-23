package com.lasthopesoftware.bluewater.client.servers.version.GivenAConnectionProviderThatDoesNotReturnProgramVersion;

import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersionProvider;
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenReceivingThePromisedProgramVersion {

	private static SemanticVersion version;

	@BeforeClass
	public static void before() throws InterruptedException, TimeoutException, ExecutionException {
		final FakeConnectionProvider connectionProvider = new FakeConnectionProvider();

		connectionProvider.mapResponse((p) -> new FakeConnectionResponseTuple(200, "<Response Status=\"OK\"></Response>".getBytes()), "Alive");

		final ProgramVersionProvider programVersionProvider = new ProgramVersionProvider(connectionProvider);
		version = new FuturePromise<>(programVersionProvider.promiseServerVersion()).get(100, TimeUnit.MILLISECONDS);
	}

	@Test
	public void thenTheServerVersionIsNull() {
		assertThat(version).isNull();
	}
}
