package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.client.connection.helpers.ConnectionTester;
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.promises.extensions.QueuedPromise;
import com.lasthopesoftware.promises.Promise;

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;

import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;

public class AccessConfigurationBuilder {

	private static final int buildConnectionTimeoutTime = 10000;
	private static final Logger mLogger = LoggerFactory.getLogger(AccessConfigurationBuilder.class);

	public static Promise<MediaServerUrlProvider> buildConfiguration(final Context context, final Library library) {
		return buildConfiguration(context, library, buildConnectionTimeoutTime);
	}

	private static Promise<MediaServerUrlProvider> buildConfiguration(final Context context, final Library library, int timeout) {
		if (library == null)
			throw new NullPointerException("The library cannot be null.");

		if (timeout <= 0) timeout = buildConnectionTimeoutTime;

		final NetworkInfo networkInfo = ConnectionInfo.getActiveNetworkInfo(context);
		return
			networkInfo != null && networkInfo.isConnected()
			? buildAccessConfiguration(library, timeout)
			: Promise.empty();
	}

	private static Promise<MediaServerUrlProvider> buildAccessConfiguration(final Library library, final int timeout) throws NullPointerException {
		if (library == null)
			throw new IllegalArgumentException("The library cannot be null");

		if (library.getAccessCode() == null)
			throw new IllegalArgumentException("The access code cannot be null");

		final String authKey = library.getAuthKey();

		final String localAccessString = parseAccessCode(library);

		if (UrlValidator.getInstance().isValid(localAccessString)) {
			final Uri url = Uri.parse(localAccessString);
			final MediaServerUrlProvider urlProvider = new MediaServerUrlProvider(authKey, url.getHost(), url.getPort());

			return
				ConnectionTester
					.doTest(new ConnectionProvider(urlProvider), timeout)
					.then(result -> {
						if (result) return new Promise<>(urlProvider);

						return promiseServerInformation(localAccessString, timeout)
							.then(xml -> promiseMediaServerUrlFromXml(xml, library, authKey, timeout));
					});
		}

		return
			promiseServerInformation(localAccessString, timeout)
				.then(xml -> promiseMediaServerUrlFromXml(xml, library, authKey, timeout));
	}

	private static String parseAccessCode(Library library){
		String localAccessString = library.getAccessCode();
		if (localAccessString.contains(".")) {
			if (!localAccessString.contains(":")) localAccessString += ":80";
			if (!localAccessString.startsWith("http://"))
				localAccessString = "http://" + localAccessString;
		}

		return localAccessString;
	}

	private static Promise<XmlElement> promiseServerInformation(String localAccessString, int timeout) {
		return new QueuedPromise<>(() -> {
			final HttpURLConnection conn = (HttpURLConnection) (new URL("http://webplay.jriver.com/libraryserver/lookup?id=" + localAccessString)).openConnection();

			conn.setConnectTimeout(timeout);
			try {
				try (InputStream is = conn.getInputStream()) {
					return Xmlwise.createXml(IOUtils.toString(is));
				}
			} finally {
				conn.disconnect();
			}
		}, AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private static Promise<MediaServerUrlProvider> promiseMediaServerUrlFromXml(XmlElement xml, Library library, String authKey, int timeout) throws XmlParseException {
		final int port = Integer.parseInt(xml.getUnique("port").getValue());

		if (!library.isLocalOnly()) {
			final MediaServerUrlProvider remoteUrlProvider = new MediaServerUrlProvider(authKey, xml.getUnique("ip").getValue(), port);
			return ConnectionTester.doTest(new ConnectionProvider(remoteUrlProvider), timeout)
				.then(testResult -> {
					if (testResult) return new Promise<>(remoteUrlProvider);

					final Collection<String> ipList = Arrays.asList(xml.getUnique("localiplist").getValue().split(","));
					return testUrls(new ArrayDeque<>(ipList), authKey, port, timeout);
				});
		}

		final Collection<String> ipList = Arrays.asList(xml.getUnique("localiplist").getValue().split(","));
		return testUrls(new ArrayDeque<>(ipList), authKey, port, timeout);
	}

	private static Promise<MediaServerUrlProvider> testUrls(Queue<String> urls, String authKey, int port, int timeout) {
		final String ipAddress = urls.poll();
		if (ipAddress == null) return Promise.empty();

		final MediaServerUrlProvider urlProvider = new MediaServerUrlProvider(authKey, ipAddress, port);
		return
			ConnectionTester.doTest(new ConnectionProvider(urlProvider), timeout)
				.then(result -> result ? new Promise<>(urlProvider) : testUrls(urls, authKey, port, timeout));
	}
}
