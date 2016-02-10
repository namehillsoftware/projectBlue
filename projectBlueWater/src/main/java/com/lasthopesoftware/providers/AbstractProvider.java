package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.vedsoft.fluent.FluentTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
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
		super(providerExecutor, params);

		this.connectionProvider = connectionProvider;
	}

	@Override
	protected final T executeInBackground(String[] params) {
		if (isCancelled()) return null;

		try {
			final HttpURLConnection connection = connectionProvider.getConnection(params);
			try {
				return getData(connection);
			} finally {
				connection.disconnect();
			}
		} catch (IOException ioe) {
			logger.error("There was an error opening the connection", ioe);
			setException(ioe);
		}

		return null;
	}

	protected T getData(HttpURLConnection connection) {
		try {
			final InputStream is = connection.getInputStream();
			try {
				return getData(is);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			logger.error("There was an error opening the input stream", e);
			setException(e);
		}

		return null;
	}

	protected abstract T getData(InputStream inputStream);
}
