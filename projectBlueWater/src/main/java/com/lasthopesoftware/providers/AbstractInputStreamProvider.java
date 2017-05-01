package com.lasthopesoftware.providers;

import android.annotation.SuppressLint;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public abstract class AbstractInputStreamProvider<T> extends AbstractConnectionProvider<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractInputStreamProvider.class);

	protected AbstractInputStreamProvider(IConnectionProvider connectionProvider, String... params) {
		super(connectionProvider, params);
	}

	@SuppressLint("NewApi")
	@Override
	protected final T getData(HttpURLConnection connection, Cancellation cancellation) throws Exception {
		try {
			try (InputStream is = connection.getInputStream()) {
				return getData(is, cancellation);
			}
		} catch (IOException e) {
			logger.error("There was an error opening the input stream", e);
			throw e;
		}
	}

	protected abstract T getData(InputStream inputStream, Cancellation cancellation) throws IOException;
}
