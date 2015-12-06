package com.lasthopesoftware.providers;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.threading.FluentTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 11/26/15.
 */
public abstract class AbstractProvider<T> extends FluentTask<String, Void, T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractProvider.class);

	private final ConnectionProvider connectionProvider;
	private static final ExecutorService providerExecutor = Executors.newSingleThreadExecutor();

	protected AbstractProvider(ConnectionProvider connectionProvider, String... params) {
		super(params);

		this.connectionProvider = connectionProvider;
	}

	@Override
	protected AsyncTask<Void, Void, T> executeTask() {
		return super.executeTask(providerExecutor);
	}

	@Override
	protected final T executeInBackground(String[] params) {
		if (isCancelled()) return null;

		try {
			final HttpURLConnection connection = connectionProvider.getConnection(params);
			try {
				try {
					return getData(this, connection);
				} catch (Exception e) {
					logger.error("There was an exception getting data", e);
					setException(e);
				}
			} finally {
				connection.disconnect();
			}
		} catch (IOException ioe) {
			logger.error("There was an error opening the connection", ioe);
			setException(ioe);
		}

		return null;
	}

	protected abstract T getData(FluentTask<String, Void, T> task, HttpURLConnection connection) throws Exception;
}
