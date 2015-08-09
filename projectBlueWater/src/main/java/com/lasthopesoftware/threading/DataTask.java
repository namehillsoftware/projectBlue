package com.lasthopesoftware.threading;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;

import java.io.InputStream;
import java.net.HttpURLConnection;

public class DataTask<TResult> extends SimpleTask<String, Void, TResult> implements IDataTask<TResult> {

	public DataTask(final OnConnectListener<TResult> listener) {
		super(new OnExecuteListener<String, Void, TResult>() {

			@Override
			public TResult onExecute(ISimpleTask<String, Void, TResult> owner, String... params) throws Exception {

				final HttpURLConnection conn = ConnectionProvider.getActiveConnection(params);
				try {
					final InputStream is = conn.getInputStream();
					try {
						return listener.onConnect(is);
					} finally {
						is.close();
					}
				} finally {
					conn.disconnect();
				}
			}
		});
	}
}
