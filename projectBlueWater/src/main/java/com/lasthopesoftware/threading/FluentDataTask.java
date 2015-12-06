package com.lasthopesoftware.threading;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public abstract class FluentDataTask<TResult> extends FluentTask<String, Void, TResult> {

	private final ConnectionProvider connectionProvider;

	public FluentDataTask(final ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}


	@Override
	protected TResult doInBackground(String... params) {
		try {
			final HttpURLConnection conn = connectionProvider.getConnection(params);
			try {
				final InputStream is = conn.getInputStream();
				try {
					return doOnConnection(is);
				} finally {
					is.close();
				}
			} catch (IOException e) {
				setException(e);
			} finally {
				conn.disconnect();
			}
		} catch (IOException e) {
			setException(e);
		}

		return null;
	}

	protected abstract TResult doOnConnection(InputStream is);
}
