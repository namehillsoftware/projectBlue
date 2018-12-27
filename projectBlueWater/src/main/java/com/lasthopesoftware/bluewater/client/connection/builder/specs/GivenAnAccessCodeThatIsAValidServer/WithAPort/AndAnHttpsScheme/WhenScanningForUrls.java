package com.lasthopesoftware.bluewater.client.connection.builder.specs.GivenAnAccessCodeThatIsAValidServer.WithAPort.AndAnHttpsScheme;

import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

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
		when(connectionTester.promiseIsConnectionPossible(argThat(a -> a.getUrlProvider().getBaseUrl().equals("https://gooPc:3504/MCWS/v1/"))))
			.thenReturn(new Promise<>(true));

		final UrlScanner urlScanner = new UrlScanner(connectionTester, mock(LookupServers.class), OkHttpFactory.getInstance());

		urlProvider = new FuturePromise<>(
			urlScanner.promiseBuiltUrlProvider(new Library()
				.setAccessCode("https://gooPc:3504"))).get();
	}

	@Test
	public void thenTheUrlProviderIsReturned() {
		assertThat(urlProvider).isNotNull();
	}

	@Test
	public void thenTheBaseUrlIsCorrect() {
		assertThat(urlProvider.getBaseUrl()).isEqualTo("https://gooPc:3504/MCWS/v1/");
	}
}
