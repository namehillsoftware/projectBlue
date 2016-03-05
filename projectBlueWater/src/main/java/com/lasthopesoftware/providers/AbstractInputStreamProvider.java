package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.servers.connection.IConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Created by david on 11/26/15.
 */
public abstract class AbstractInputStreamProvider<T> extends AbstractConnectionProvider<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractInputStreamProvider.class);

	protected AbstractInputStreamProvider(IConnectionProvider connectionProvider, String... params) {
		super(connectionProvider, params);
	}

	protected final T getData(HttpURLConnection connection) {
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
