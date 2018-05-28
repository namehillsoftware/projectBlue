package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Patterns;

import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester;
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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

	public static Promise<MediaServerUrlProvider> buildConfiguration(final Context context, final Library library) {
		return buildConfiguration(context, library, buildConnectionTimeoutTime);
	}

	private static Promise<MediaServerUrlProvider> buildConfiguration(final Context context, final Library library, int timeout) {
		if (library == null)
			throw new IllegalArgumentException("The library cannot be null.");

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
		if (Patterns.WEB_URL.matcher(localAccessString).matches()) {
			final Uri url = Uri.parse(localAccessString);
			final MediaServerUrlProvider urlProvider;
			try {
				urlProvider = new MediaServerUrlProvider(authKey, "http", url.getHost(), url.getPort());
			} catch (MalformedURLException e) {
				return new Promise<>(e);
			}

			return
				ConnectionTester
					.doTest(new ConnectionProvider(urlProvider), timeout)
					.eventually(result -> result
						? new Promise<>(urlProvider)
						: promiseServerInformation(localAccessString, timeout)
							.eventually(xml -> promiseMediaServerUrlFromXml(xml, library, authKey, timeout)));
		}

		return
			promiseServerInformation(localAccessString, timeout)
				.eventually(xml -> promiseMediaServerUrlFromXml(xml, library, authKey, timeout));
	}

	private static String parseAccessCode(Library library) {
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
		final int httpsPort = Integer.parseInt(xml.getUnique("https_port").getValue());

		if (!library.isLocalOnly()) {
			final MediaServerUrlProvider remoteUrlProvider;
			try {
				remoteUrlProvider = new MediaServerUrlProvider(authKey, "http", xml.getUnique("ip").getValue(), port);
			} catch (MalformedURLException e) {
				return new Promise<>(e);
			}
			return ConnectionTester.doTest(new ConnectionProvider(remoteUrlProvider), timeout)
				.eventually(testResult -> {
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

		final MediaServerUrlProvider urlProvider;
		try {
			urlProvider = new MediaServerUrlProvider(authKey, "http", ipAddress, port);
		} catch (MalformedURLException e) {
			return new Promise<>(e);
		}
		return
			ConnectionTester.doTest(new ConnectionProvider(urlProvider), timeout)
				.eventually(result -> result ? new Promise<>(urlProvider) : testUrls(urls, authKey, port, timeout));
	}
}
