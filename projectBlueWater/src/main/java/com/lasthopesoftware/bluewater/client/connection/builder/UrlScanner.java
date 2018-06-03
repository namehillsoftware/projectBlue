package com.lasthopesoftware.bluewater.client.connection.builder;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

import org.apache.commons.codec.binary.Hex;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Queue;

import javax.net.ssl.SSLSocketFactory;

public class UrlScanner implements BuildUrlProviders {

	private final TestConnections connectionTester;
	private final LookupServers serverLookup;

	public UrlScanner(TestConnections connectionTester, LookupServers serverLookup) {
		this.connectionTester = connectionTester;
		this.serverLookup = serverLookup;
	}

	@Override
	public Promise<IUrlProvider> promiseBuiltUrlProvider(Library library) {
		if (library == null)
			return new Promise<>(new IllegalArgumentException("The library cannot be null"));

		if (library.getAccessCode() == null)
			return new Promise<>(new IllegalArgumentException("The access code cannot be null"));

		final String authKey = library.getAuthKey();
		final MediaServerUrlProvider mediaServerUrlProvider;
		try {
			mediaServerUrlProvider = new MediaServerUrlProvider(
				authKey,
				parseAccessCode(library.getAccessCode()));
		} catch (MalformedURLException e) {
			return new Promise<>(e);
		}

		return connectionTester.promiseIsConnectionPossible(new ConnectionProvider(mediaServerUrlProvider, (SSLSocketFactory) SSLSocketFactory.getDefault()))
			.eventually(isValid -> isValid
				? new Promise<>(mediaServerUrlProvider)
				: serverLookup.promiseServerInformation(library)
				.eventually(info -> {
					final int httpPort = info.getHttpPort();
					final String remoteIp = info.getRemoteIp();

					final Queue<IUrlProvider> mediaServerUrlProvidersQueue = new ArrayDeque<>();
					mediaServerUrlProvidersQueue.offer(new MediaServerUrlProvider(
						authKey,
						remoteIp,
						httpPort));

					final Integer httpsPort = info.getHttpsPort();
					if (httpsPort != null) {
//						final SelfSignedTrustManager trustManager = new SelfSignedTrustManager());
//
//						final SSLContext sslContext = SSLContext.getInstance("TLS");
//						sslContext.init(null, new TrustManager[] { trustManager }, null);

						mediaServerUrlProvidersQueue.offer(new MediaServerUrlProvider(
							authKey,
							remoteIp,
							httpsPort,
							Hex.decodeHex(info.getCertificateFingerprint())));
					}

					for (String ip : info.getLocalIps()) {
						mediaServerUrlProvidersQueue.offer(new MediaServerUrlProvider(
							authKey,
							ip,
							httpPort));
					}

					return testUrls(mediaServerUrlProvidersQueue);
				}));
	}

	private Promise<IUrlProvider> testUrls(Queue<IUrlProvider> urls) {
		final IUrlProvider urlProvider = urls.poll();
		if (urlProvider == null) return Promise.empty();

		return connectionTester
			.promiseIsConnectionPossible(new ConnectionProvider(urlProvider, (SSLSocketFactory) SSLSocketFactory.getDefault()))
			.eventually(result -> result ? new Promise<>(urlProvider) : testUrls(urls));
	}

	private static URL parseAccessCode(String accessCode) throws MalformedURLException {
		String url = accessCode;

		String scheme = "http";
		if (url.startsWith("http://"))
			url = url.replaceFirst("http://", "");

		if (url.startsWith("https://")) {
			url = url.replaceFirst("https://", "");
			scheme = "https";
		}

		final String[] urlParts = url.split(":", 2);

		final int port =
			urlParts.length > 1 && isPositiveInteger(urlParts[1])
				? Integer.parseInt(urlParts[1])
				: 80;

		return new URL(scheme, urlParts[0], port, "");
	}

	private static boolean isPositiveInteger(String string) {
		for (final char c : string.toCharArray())
			if (!Character.isDigit(c)) return false;

		return true;
	}
}
