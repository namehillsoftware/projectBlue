package com.lasthopesoftware.bluewater.client.connection.builder.GivenANullLibrary;

import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers;
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.lasthopesoftware.resources.strings.EncodeToBase64;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenScanningForUrls {

	private static IllegalArgumentException illegalArgumentException;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException {
		final UrlScanner urlScanner = new UrlScanner(
			mock(EncodeToBase64.class),
			mock(TestConnections.class),
			mock(LookupServers.class),
			mock(ProvideOkHttpClients.class));

		try {
			new FuturePromise<>(urlScanner.promiseBuiltUrlProvider(null)).get();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof IllegalArgumentException)
				illegalArgumentException = (IllegalArgumentException) e.getCause();
			else
				throw e;
		}
	}

	@Test
	public void thenAnIllegalArgumentExceptionIsThrown() {
		assertThat(illegalArgumentException).isNotNull();
	}

	@Test
	public void thenTheExceptionMentionsTheLibrary() {
		assertThat(illegalArgumentException.getMessage()).isEqualTo("The library cannot be null");
	}
}
