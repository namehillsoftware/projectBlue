package com.lasthopesoftware.bluewater.client.servers.version.specs.GivenAConnectionProviderThatDoesNotReturnProgramVersion;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersion;
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersionProvider;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenReceivingThePromisedProgramVersion {

	private static ProgramVersion version;

	@BeforeClass
	public static void before() throws IOException, InterruptedException {
		final IUrlProvider urlProvider = mock(IUrlProvider.class);
		when(urlProvider.getBaseUrl()).thenReturn("");

		final IConnectionProvider connectionProvider = mock(IConnectionProvider.class);
		when(connectionProvider.getUrlProvider()).thenReturn(urlProvider);

		final HttpURLConnection urlConnection = mock(HttpURLConnection.class);
		when(urlConnection.getInputStream())
			.thenReturn(new ByteArrayInputStream("<Response Status=\"OK\"></Response>".getBytes()));
		when(connectionProvider.getConnection("Alive")).thenReturn(urlConnection);

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
