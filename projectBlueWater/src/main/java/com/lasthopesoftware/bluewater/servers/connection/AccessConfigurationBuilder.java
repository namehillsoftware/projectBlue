package com.lasthopesoftware.bluewater.servers.connection;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.servers.connection.helpers.ConnectionTester;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xmlwise.XmlElement;
import xmlwise.Xmlwise;

/**
 * Created by david on 8/8/15.
 */
public class AccessConfigurationBuilder {

	private static final int buildConnectionTimeoutTime = 10000;
	private static final Logger mLogger = LoggerFactory.getLogger(AccessConfigurationBuilder.class);

	public static void buildConfiguration(final Context context, final Library library, final TwoParameterRunnable<FluentTask<Void, Void, AccessConfiguration>, AccessConfiguration> onBuildComplete) {
		buildConfiguration(context, library, buildConnectionTimeoutTime, onBuildComplete);
	}

	private static void buildConfiguration(final Context context, final Library library, int timeout, final TwoParameterRunnable<FluentTask<Void, Void, AccessConfiguration>, AccessConfiguration> onBuildComplete) throws NullPointerException {
		if (library == null)
			throw new NullPointerException("The library cannot be null.");

		if (timeout <= 0) timeout = buildConnectionTimeoutTime;

		final NetworkInfo networkInfo = ConnectionInfo.getActiveNetworkInfo(context);
		if (networkInfo == null || !networkInfo.isConnected()) {
			executeReturnNullTask(onBuildComplete);
			return;
		}

		buildAccessConfiguration(library, timeout, new TwoParameterRunnable<FluentTask<Void, Void, AccessConfiguration>, AccessConfiguration>() {

			@Override
			public void run(final FluentTask<Void, Void, AccessConfiguration> builderOwner, final AccessConfiguration accessConfiguration) {
				if (accessConfiguration == null) {
					executeReturnNullTask(onBuildComplete);
					return;
				}

				if (onBuildComplete != null)
					onBuildComplete.run(builderOwner, accessConfiguration);
			}
		});
	}

	private static void executeReturnNullTask(TwoParameterRunnable<FluentTask<Void, Void, AccessConfiguration>, AccessConfiguration> onReturnFalseListener) {
		final FluentTask<Void, Void, AccessConfiguration> returnFalseTask = new FluentTask<Void, Void, AccessConfiguration>() {
			@Override
			protected AccessConfiguration executeInBackground(Void... params) {
				return null;
			}
		};

		returnFalseTask
			.onComplete(onReturnFalseListener)
			.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private static void buildAccessConfiguration(final Library library, final int timeout, TwoParameterRunnable<FluentTask<Void, Void, AccessConfiguration>, AccessConfiguration> onGetAccessComplete) throws NullPointerException {
		if (library == null)
			throw new IllegalArgumentException("The library cannot be null");

		if (library.getAccessCode() == null)
			throw new IllegalArgumentException("The access code cannot be null");

		final FluentTask<Void, Void, AccessConfiguration> mediaCenterAccessTask = new FluentTask<Void, Void, AccessConfiguration>() {
			@Override
			protected AccessConfiguration executeInBackground(Void... params) {
				try {
					final AccessConfiguration accessDao = new AccessConfiguration(library.getId(), library.getAuthKey());
					String localAccessString = library.getAccessCode();
					if (localAccessString.contains(".")) {
						if (!localAccessString.contains(":")) localAccessString += ":80";
						if (!localAccessString.startsWith("http://")) localAccessString = "http://" + localAccessString;
					}

					if (UrlValidator.getInstance().isValid(localAccessString)) {
						final Uri jrUrl = Uri.parse(localAccessString);
						accessDao.setIpAddress(jrUrl.getHost());
						accessDao.setPort(jrUrl.getPort());
						accessDao.setStatus(true);

						return accessDao;
					}

					final HttpURLConnection conn = (HttpURLConnection)(new URL("http://webplay.jriver.com/libraryserver/lookup?id=" + localAccessString)).openConnection();

					conn.setConnectTimeout(timeout);
					try {
						final InputStream is = conn.getInputStream();
						try {
							final XmlElement xml = Xmlwise.createXml(IOUtils.toString(is));
							accessDao.setStatus(xml.getAttribute("Status").equalsIgnoreCase("OK"));
							accessDao.setPort(Integer.parseInt(xml.getUnique("port").getValue()));

							final List<String> ipAddresses = new ArrayList<>(Arrays.asList(xml.getUnique("localiplist").getValue().split(",")));
							if (!library.isLocalOnly())
								ipAddresses.add(xml.getUnique("ip").getValue());

							for (String ipAddress : ipAddresses) {
								accessDao.setIpAddress(ipAddress);
								if (ConnectionTester.doTest(new ConnectionProvider(accessDao), timeout))
									return accessDao;
							}

						} finally {
							is.close();
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

		if (onGetAccessComplete != null)
			mediaCenterAccessTask.onComplete(onGetAccessComplete);

		mediaCenterAccessTask.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}
