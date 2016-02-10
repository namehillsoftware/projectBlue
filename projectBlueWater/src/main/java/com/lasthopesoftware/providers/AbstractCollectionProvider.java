package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCollectionProvider<T> extends AbstractProvider<List<T>> {
	protected AbstractCollectionProvider(ConnectionProvider connectionProvider, String... params) {
        super(connectionProvider, params);
	}

	@Override
	protected List<T> getData(HttpURLConnection connection) {
		final List<T> result = super.getData(connection);
		return result != null ? result : new ArrayList<>();
	}
}
