package com.lasthopesoftware.bluewater.client.connection.helpers;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.vedsoft.fluent.FluentDeterministicTask;
import com.vedsoft.fluent.FluentSpecifiedTask;
import com.vedsoft.fluent.IFluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Created by david on 8/10/15.
 */
public class ConnectionTester {

	private static final int stdTimeoutTime = 30000;

	private static final Logger mLogger = LoggerFactory.getLogger(ConnectionTester.class);

	public static void doTest(ConnectionProvider connectionProvider, TwoParameterRunnable<IFluentTask<Void, Void, Boolean>, Boolean> onTestComplete) {
		doTest(connectionProvider, stdTimeoutTime, onTestComplete);
	}

	public static void doTest(final ConnectionProvider connectionProvider, final int timeout, TwoParameterRunnable<IFluentTask<Void, Void, Boolean>, Boolean> onTestComplete) {
		final FluentDeterministicTask<Boolean> connectionTestTask = new FluentDeterministicTask<Boolean>() {
			@Override
			protected Boolean executeInBackground() {
				return doTest(connectionProvider, timeout);
			}
		};

		if (onTestComplete != null)
			connectionTestTask.onComplete(onTestComplete);

		connectionTestTask.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static boolean doTest(final ConnectionProvider connectionProvider) {
		return doTest(connectionProvider, stdTimeoutTime);
	}

	public static boolean doTest(final ConnectionProvider connectionProvider, final int timeout) {
		try {

			final HttpURLConnection conn = connectionProvider.getConnection("Alive");

			if (conn == null) return Boolean.FALSE;

			try {
				conn.setConnectTimeout(timeout);

				final InputStream is = conn.getInputStream();
				try {
					final StandardRequest responseDao = StandardRequest.fromInputStream(is);

					return responseDao != null && responseDao.isStatus();
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						mLogger.error("Error closing connection, device failure?", e);
					}
				}
			} catch (IOException e) {
				mLogger.info("Unable to get input stream, connection does likely not exist", e);
			} catch (IllegalArgumentException e) {
				mLogger.warn("Illegal argument passed in", e);
			} finally {
				conn.disconnect();
			}
		} catch (IOException e) {
			mLogger.warn("Error getting a connection", e);
		}

		return false;
	}
}
