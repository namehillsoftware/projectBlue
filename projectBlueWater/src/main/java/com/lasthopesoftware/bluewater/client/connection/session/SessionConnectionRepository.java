package com.lasthopesoftware.bluewater.client.connection.session;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.namehillsoftware.handoff.promises.Promise;

public class SessionConnectionRepository {

	private final Object connectionProviderSync = new Object();
	private volatile SessionConnectionTuple sessionConnectionTuple;

	public Promise<SessionConnectionRepository> setSessionConnection(SessionConnectionTuple sessionConnectionTuple) {
		synchronized (connectionProviderSync) {
			this.sessionConnectionTuple = sessionConnectionTuple;
		}

		return new Promise<>(this);
	}

	public Promise<SessionConnectionTuple> promiseSessionConnection() {
		synchronized (connectionProviderSync) {
			return new Promise<>(sessionConnectionTuple);
		}
	}

	public class SessionConnectionTuple {
		public final int libraryId;
		public final IConnectionProvider connectionProvider;

		public SessionConnectionTuple(int libraryId, IConnectionProvider connectionProvider) {
			this.libraryId = libraryId;
			this.connectionProvider = connectionProvider;
		}
	}
}
