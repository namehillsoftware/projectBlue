package com.lasthopesoftware.bluewater.client.connection.builder.specs.GivenServerIsFoundViaLookup.AndLocalOnlyIsSelected;

import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenScanningForUrls {

	private static IUrlProvider urlProvider;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException {
		final TestConnections connectionTester = mock(TestConnections.class);
		when(connectionTester.promiseIsConnectionPossible(any()))
			.thenReturn(new Promise<>(false));

		when(connectionTester.promiseIsConnectionPossible(argThat(a -> Arrays.asList("http://192.168.1.56:143/MCWS/v1/", "http://1.2.3.4:143/MCWS/v1/").contains(a.getUrlProvider().getBaseUrl()))))
			.thenReturn(new Promise<>(true));

		final LookupServers serverLookup = mock(LookupServers.class);
		when(serverLookup.promiseServerInformation(argThat(a -> "gooPc".equals(a.getAccessCode()))))
			.thenReturn(new Promise<>(
				new ServerInfo()
					.setRemoteIp("1.2.3.4")
					.setHttpPort(143)
					.setLocalIps(Arrays.asList(
						"53.24.19.245",
						"192.168.1.56"))));

		final UrlScanner urlScanner = new UrlScanner(
			connectionTester,
			serverLookup);

		urlProvider = new FuturePromise<>(
			urlScanner.promiseBuiltUrlProvider(new Library()
				.setAccessCode("gooPc")
				.setLocalOnly(true))).get();
	}

	@Test
	public void thenTheUrlProviderIsReturned() {
		assertThat(urlProvider).isNotNull();
	}

	@Test
	public void thenTheBaseUrlIsCorrect() {
		assertThat(urlProvider.getBaseUrl()).isEqualTo("http://192.168.1.56:143/MCWS/v1/");
	}
}
