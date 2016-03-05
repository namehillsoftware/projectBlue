package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.servers.connection.IConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Created by david on 11/26/15.
 */
public abstract class AbstractConnectionProvider<T> extends AbstractProvider<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractConnectionProvider.class);

	protected AbstractConnectionProvider(IConnectionProvider connectionProvider, String... params) {
		super(connectionProvider, params);
	}

	@Override
	protected final T getData(IConnectionProvider connectionProvider, String[] params) {
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

	protected abstract T getData(HttpURLConnection connection);
}
