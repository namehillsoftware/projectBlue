package com.lasthopesoftware.bluewater.client.connection.builder;

import android.util.Base64;
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers;
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;

public class UrlScanner implements BuildUrlProviders {

	private final TestConnections connectionTester;
	private final LookupServers serverLookup;
	private final ProvideOkHttpClients okHttpClients;

	public UrlScanner(TestConnections connectionTester, LookupServers serverLookup, ProvideOkHttpClients okHttpClients) {
		this.connectionTester = connectionTester;
		this.serverLookup = serverLookup;
		this.okHttpClients = okHttpClients;
	}

	@Override
	public Promise<IUrlProvider> promiseBuiltUrlProvider(Library library) {
		if (library == null)
			return new Promise<>(new IllegalArgumentException("The library cannot be null"));

		if (library.getAccessCode() == null)
			return new Promise<>(new IllegalArgumentException("The access code cannot be null"));

		final String authKey = library.getUserName() != null
			? Base64.encodeToString((library.getUserName() + ":" + library.getPassword()).getBytes(), Base64.DEFAULT)
			: null;

		final MediaServerUrlProvider mediaServerUrlProvider;
		try {
			mediaServerUrlProvider = new MediaServerUrlProvider(
				authKey,
				parseAccessCode(library.getAccessCode()));
		} catch (MalformedURLException e) {
			return new Promise<>(e);
		}

		return connectionTester.promiseIsConnectionPossible(new ConnectionProvider(mediaServerUrlProvider, okHttpClients))
			.eventually(isValid -> isValid
				? new Promise<>(mediaServerUrlProvider)
				: serverLookup.promiseServerInformation(library)
				.eventually(info -> {
					final int httpPort = info.getHttpPort();
					final String remoteIp = info.getRemoteIp();

					final Queue<IUrlProvider> mediaServerUrlProvidersQueue = new LinkedList<>();

					if (!library.isLocalOnly()) {
						final Integer httpsPort = info.getHttpsPort();
						if (httpsPort != null) {
							final String certificateFingerprint = info.getCertificateFingerprint();
							mediaServerUrlProvidersQueue.offer(new MediaServerUrlProvider(
								authKey,
								remoteIp,
								httpsPort,
								certificateFingerprint != null
									? decodeHex(certificateFingerprint.toCharArray())
									: new byte[0]));
						}

						mediaServerUrlProvidersQueue.offer(new MediaServerUrlProvider(
							authKey,
							remoteIp,
							httpPort));
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
			.promiseIsConnectionPossible(new ConnectionProvider(urlProvider, okHttpClients))
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

	private static byte[] decodeHex(final char[] data) {

		final int len = data.length;

		if ((len & 0x01) != 0) {
			return new byte[0];
		}

		final byte[] out = new byte[len >> 1];

		// two characters form the hex value.
		for (int i = 0, j = 0; j < len; i++) {
			int f = Character.digit(data[j], 16) << 4;
			j++;
			f = f | Character.digit(data[j], 16);
			j++;
			out[i] = (byte) (f & 0xFF);
		}

		return out;
	}
}
