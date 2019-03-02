package com.lasthopesoftware.bluewater.client.connection.builder.specs.GivenSecureServerIsFoundViaLookup;

import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.resources.strings.EncodeToBase64;
import com.namehillsoftware.handoff.promises.Promise;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
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

		when(connectionTester.promiseIsConnectionPossible(argThat(a ->
			"https://1.2.3.4:452/MCWS/v1/".equals(a.getUrlProvider().getBaseUrl())
			|| "http://1.2.3.4:143/MCWS/v1/".equals(a.getUrlProvider().getBaseUrl()))))
			.thenReturn(new Promise<>(true));

		final LookupServers serverLookup = mock(LookupServers.class);
		when(serverLookup.promiseServerInformation(argThat(a -> "gooPc".equals(a.getAccessCode()))))
			.thenReturn(new Promise<>(new ServerInfo()
				.setRemoteIp("1.2.3.4")
				.setHttpPort(143)
				.setHttpsPort(452)
				.setCertificateFingerprint("2386166660562C5AAA1253B2BED7C2483F9C2D45")));

		final UrlScanner urlScanner = new UrlScanner(
			mock(EncodeToBase64.class),
			connectionTester,
			serverLookup,
			OkHttpFactory.getInstance());

		urlProvider = new FuturePromise<>(
			urlScanner.promiseBuiltUrlProvider(new Library()
				.setAccessCode("gooPc"))).get();
	}

	@Test
	public void thenTheUrlProviderIsReturned() {
		assertThat(urlProvider).isNotNull();
	}

	@Test
	public void thenTheBaseUrlIsCorrect() {
		assertThat(urlProvider.getBaseUrl()).isEqualTo("https://1.2.3.4:452/MCWS/v1/");
	}

	@Test
	public void thenTheCertificateFingerprintIsCorrect() throws DecoderException {
		assertThat(urlProvider.getCertificateFingerprint()).isEqualTo(Hex.decodeHex("2386166660562C5AAA1253B2BED7C2483F9C2D45"));
	}
}
