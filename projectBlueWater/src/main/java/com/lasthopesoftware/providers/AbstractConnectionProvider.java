package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.promises.queued.cancellation.CancellationToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;

public abstract class AbstractConnectionProvider<T> extends AbstractProvider<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractConnectionProvider.class);

	protected AbstractConnectionProvider(IConnectionProvider connectionProvider, String... params) {
		super(connectionProvider, params);
	}

	@Override
	protected final T getData(IConnectionProvider connectionProvider, CancellationToken cancellation, String... params) throws Exception {
		if (cancellation.isCancelled()) return null;

		try {
			final HttpURLConnection connection = connectionProvider.getConnection(params);
			try {
				return getData(connection, cancellation);
			} finally {
				connection.disconnect();
			}
		} catch (IOException ioe) {
			logger.error("There was an error opening the connection", ioe);
		}

		return null;
	}

	protected abstract T getData(HttpURLConnection connection, CancellationToken cancellation) throws Exception;
}
