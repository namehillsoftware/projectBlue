package com.lasthopesoftware.bluewater.client.connection.testing;

import android.os.AsyncTask;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class ConnectionTester implements TestConnections {

	private static final Duration stdTimeoutTime = Duration.millis(30000);

	private static final Logger mLogger = LoggerFactory.getLogger(ConnectionTester.class);

	@Override
	public Promise<Boolean> promiseIsConnectionPossible(IConnectionProvider connectionProvider) {
		return promiseIsConnectionPossible(connectionProvider, stdTimeoutTime);
	}

	@Override
	public Promise<Boolean> promiseIsConnectionPossible(IConnectionProvider connectionProvider, Duration timeout) {
		return new QueuedPromise<>(() -> doTestSynchronously(connectionProvider, timeout), AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private boolean doTestSynchronously(final IConnectionProvider connectionProvider, Duration timeout) {
		try {

			final HttpURLConnection conn = connectionProvider.getConnection("Alive");

			if (conn == null) return Boolean.FALSE;

			try {
				conn.setConnectTimeout((int) timeout.getMillis());

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
