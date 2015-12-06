package com.lasthopesoftware.providers;

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
public abstract class AbstractProvider<T> extends FluentTask<Void, Void, T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractProvider.class);

	private final ConnectionProvider connectionProvider;
	private final String[] params;
	private static final ExecutorService providerExecutor = Executors.newSingleThreadExecutor();

	protected AbstractProvider(ConnectionProvider connectionProvider, String... params) {
		this.connectionProvider = connectionProvider;
		this.params = params;

	}

	@Override
	public FluentTask<Void, Void, T> execute(Void... params) {
		return super.execute(providerExecutor, params);
	}

	@Override
	protected final T doInBackground(Void... params) {
		if (isCancelled()) return null;

		try {
			final HttpURLConnection connection = connectionProvider.getConnection(this.params);
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

	protected abstract T getData(FluentTask<Void, Void, T> task, HttpURLConnection connection) throws Exception;
}
