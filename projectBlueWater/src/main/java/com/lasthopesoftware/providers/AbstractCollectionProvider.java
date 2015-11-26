package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;

import java.util.List;

public abstract class AbstractCollectionProvider<T> extends AbstractProvider<List<T>> {
	protected AbstractCollectionProvider(ConnectionProvider connectionProvider, String... params) {
        super(connectionProvider, params);
	}
}
