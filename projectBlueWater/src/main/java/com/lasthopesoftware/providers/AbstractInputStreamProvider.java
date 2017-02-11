package com.lasthopesoftware.providers;

import android.annotation.SuppressLint;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;

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

	@SuppressLint("NewApi")
	protected final T getData(HttpURLConnection connection) {
		try {
			try (InputStream is = connection.getInputStream()) {
				return getData(is);
			}
		} catch (IOException e) {
			logger.error("There was an error opening the input stream", e);
			setException(e);
		}

		return null;
	}

	protected abstract T getData(InputStream inputStream);
}
