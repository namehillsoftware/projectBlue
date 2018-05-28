package com.lasthopesoftware.bluewater.client.connection.builder.specs.GivenAValidServer;

import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenScanningForUrls {

	private static IUrlProvider urlProvider;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException {
		final TestConnections connectionTester = mock(TestConnections.class);
		when(connectionTester.promiseIsConnectionPossible(argThat(a -> a.getUrlProvider().getBaseUrl().equals("http://gooPc:80/MCWS/v1"))))
			.thenReturn(new Promise<>(true));

		final UrlScanner urlScanner = new UrlScanner(connectionTester);

		urlProvider = new FuturePromise<>(
			urlScanner.promiseBuiltUrlProvider(new Library()
				.setAccessCode("gooPc"))).get();
	}

	@Test
	public void thenTheUrlProviderIsReturned() {
		assertThat(urlProvider).isNotNull();
	}
}
