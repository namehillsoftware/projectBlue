package com.lasthopesoftware.bluewater.data.service.access;

import java.io.InputStream;
import java.net.HttpURLConnection;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionManager;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

public class DataTask<TResult> extends SimpleTask<String, Void, TResult> implements IDataTask<TResult> {

	public DataTask(final OnConnectListener<TResult> listener) {
		super(new OnExecuteListener<String, Void, TResult>() {

			@Override
			public TResult onExecute(ISimpleTask<String, Void, TResult> owner, String... params) throws Exception {

				final HttpURLConnection conn = ConnectionManager.getConnection(params);
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
