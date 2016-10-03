package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.client.connection.helpers.ConnectionTester;
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.vedsoft.fluent.FluentCallable;
import com.vedsoft.fluent.IFluentTask;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import xmlwise.XmlElement;
import xmlwise.Xmlwise;

/**
 * Created by david on 8/8/15.
 */
public class AccessConfigurationBuilder {

	private static final int buildConnectionTimeoutTime = 10000;
	private static final Logger mLogger = LoggerFactory.getLogger(AccessConfigurationBuilder.class);

	public static void buildConfiguration(final Context context, final Library library, final OneParameterRunnable<MediaServerUrlProvider> onBuildComplete) {
		buildConfiguration(context, library, buildConnectionTimeoutTime, onBuildComplete);
	}

	private static void buildConfiguration(final Context context, final Library library, int timeout, final OneParameterRunnable<MediaServerUrlProvider> onBuildComplete) throws NullPointerException {
		if (library == null)
			throw new NullPointerException("The library cannot be null.");

		if (timeout <= 0) timeout = buildConnectionTimeoutTime;

		final NetworkInfo networkInfo = ConnectionInfo.getActiveNetworkInfo(context);
		if (networkInfo == null || !networkInfo.isConnected()) {
			executeReturnNullTask(onBuildComplete);
			return;
		}

		buildAccessConfiguration(library, timeout, (urlProvider) -> {
			if (urlProvider == null) {
				executeReturnNullTask(onBuildComplete);
				return;
			}

			if (onBuildComplete != null)
				onBuildComplete.run(urlProvider);
		});
	}

	private static void executeReturnNullTask(OneParameterRunnable<MediaServerUrlProvider> onReturnFalseListener) {
		final FluentCallable<MediaServerUrlProvider> returnFalseTask = new FluentCallable<MediaServerUrlProvider>() {
			@Override
			protected MediaServerUrlProvider executeInBackground() {
				return null;
			}
		};

		returnFalseTask
			.onComplete(onReturnFalseListener)
			.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private static void buildAccessConfiguration(final Library library, final int timeout, OneParameterRunnable<MediaServerUrlProvider> onGetAccessComplete) throws NullPointerException {
		if (library == null)
			throw new IllegalArgumentException("The library cannot be null");

		if (library.getAccessCode() == null)
			throw new IllegalArgumentException("The access code cannot be null");

		final FluentCallable<MediaServerUrlProvider> mediaCenterAccessTask = new FluentCallable<MediaServerUrlProvider>() {
			@Override
			protected MediaServerUrlProvider executeInBackground() {
				try {
					final String authKey = library.getAuthKey();

					String localAccessString = library.getAccessCode();
					if (localAccessString.contains(".")) {
						if (!localAccessString.contains(":")) localAccessString += ":80";
						if (!localAccessString.startsWith("http://")) localAccessString = "http://" + localAccessString;
					}

					if (UrlValidator.getInstance().isValid(localAccessString)) {
						final Uri url = Uri.parse(localAccessString);
						final MediaServerUrlProvider urlProvider = new MediaServerUrlProvider(authKey, url.getHost(), url.getPort());
						if (ConnectionTester.doTest(new ConnectionProvider(urlProvider), timeout))
							return urlProvider;
					}

					final HttpURLConnection conn = (HttpURLConnection)(new URL("http://webplay.jriver.com/libraryserver/lookup?id=" + localAccessString)).openConnection();

					conn.setConnectTimeout(timeout);
					try {
						try (InputStream is = conn.getInputStream()) {
							final XmlElement xml = Xmlwise.createXml(IOUtils.toString(is));
							final int port = Integer.parseInt(xml.getUnique("port").getValue());

							if (!library.isLocalOnly()) {
								final MediaServerUrlProvider urlProvider = new MediaServerUrlProvider(authKey, xml.getUnique("ip").getValue(), port);
								if (ConnectionTester.doTest(new ConnectionProvider(urlProvider), timeout))
									return urlProvider;
							}

							for (String ipAddress : xml.getUnique("localiplist").getValue().split(",")) {
								final MediaServerUrlProvider urlProvider = new MediaServerUrlProvider(authKey, ipAddress, port);
								if (ConnectionTester.doTest(new ConnectionProvider(urlProvider), timeout))
									return urlProvider;
							}
						}
					} finally {
						conn.disconnect();
					}
				} catch (IOException i) {
					mLogger.error(i.getMessage());
				} catch (Exception e) {
					mLogger.warn(e.toString());
				}

				return null;
			}
		};

		mediaCenterAccessTask
				.onComplete(onGetAccessComplete)
				.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}
