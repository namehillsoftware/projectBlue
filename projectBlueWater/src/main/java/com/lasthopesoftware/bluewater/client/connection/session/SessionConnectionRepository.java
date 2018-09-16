package com.lasthopesoftware.bluewater.client.connection.session;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.namehillsoftware.handoff.promises.Promise;

public class SessionConnectionRepository {

	private final Object connectionProviderSync = new Object();
	private volatile IConnectionProvider connectionProvider;

	public Promise<SessionConnectionRepository> setSessionConnection(IConnectionProvider connectionProvider) {
		synchronized (connectionProviderSync) {
			this.connectionProvider = connectionProvider;
		}

		return new Promise<>(this);
	}

	public Promise<IConnectionProvider> promiseSessionConnection() {
		synchronized (connectionProviderSync) {
			return new Promise<>(connectionProvider);
		}
	}
}
