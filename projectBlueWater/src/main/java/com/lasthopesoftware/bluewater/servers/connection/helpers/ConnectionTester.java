package com.lasthopesoftware.bluewater.servers.connection.helpers;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

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

	public static void doTest(ConnectionProvider connectionProvider, ISimpleTask.OnCompleteListener<Integer, Void, Boolean> onTestComplete) {
		doTest(connectionProvider, stdTimeoutTime, onTestComplete);
	}

	public static void doTest(final ConnectionProvider connectionProvider, final int timeout, ISimpleTask.OnCompleteListener<Integer, Void, Boolean> onTestComplete) {
		final SimpleTask<Integer, Void, Boolean> connectionTestTask = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Integer, Void, Boolean>() {

			@Override
			public Boolean onExecute(ISimpleTask<Integer, Void, Boolean> owner, Integer... params) throws Exception {
				Boolean result = Boolean.FALSE;

				final HttpURLConnection conn = connectionProvider.getConnection("Alive");
				if (conn == null) return result;

				try {
					conn.setConnectTimeout(timeout);
					final InputStream is = conn.getInputStream();
					try {
						final StandardRequest responseDao = StandardRequest.fromInputStream(is);

						result = responseDao != null && responseDao.isStatus();
					} finally {
						is.close();
					}
				} catch (IOException | IllegalArgumentException e) {
					mLogger.warn(e.getMessage());
				} finally {
					conn.disconnect();
				}

				return result;
			}

		});

		if (onTestComplete != null)
			connectionTestTask.addOnCompleteListener(onTestComplete);

		connectionTestTask.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}
