package com.lasthopesoftware.bluewater.client.servers.version.GivenAStandardConnectionProvider;

import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersionProvider;
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenReceivingThePromisedProgramVersion {

	private static SemanticVersion version;
	private static SemanticVersion expectedVersion;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException, TimeoutException {
		final IUrlProvider urlProvider = mock(IUrlProvider.class);
		when(urlProvider.getBaseUrl()).thenReturn("");

		final FakeConnectionProvider connectionProvider = new FakeConnectionProvider();

		final Random random = new Random();
		expectedVersion = new SemanticVersion(random.nextInt(), random.nextInt(), random.nextInt());
		connectionProvider.mapResponse(p -> new FakeConnectionResponseTuple(200,
				("<Response Status=\"OK\">" +
					"<Item Name=\"RuntimeGUID\">{7FF5918E-9FDE-4D4D-9AE7-62DFFDD64397}</Item>" +
					"<Item Name=\"LibraryVersion\">24</Item><Item Name=\"ProgramName\">JRiver Media Center</Item>" +
					"<Item Name=\"ProgramVersion\">" + expectedVersion + "</Item>" +
					"<Item Name=\"FriendlyName\">Media-Pc</Item>" +
					"<Item Name=\"AccessKey\">nIpfQr</Item>" +
				"</Response>").getBytes()), "Alive");

		final ProgramVersionProvider programVersionProvider = new ProgramVersionProvider(connectionProvider);
		version = new FuturePromise<>(programVersionProvider.promiseServerVersion()).get(100, TimeUnit.MILLISECONDS);
	}

	@Test
	public void thenTheServerVersionIsCorrect() {
		assertThat(version).isEqualTo(expectedVersion);
	}
}
