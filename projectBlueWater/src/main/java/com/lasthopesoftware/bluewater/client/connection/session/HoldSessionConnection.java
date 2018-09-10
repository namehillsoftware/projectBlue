package com.lasthopesoftware.bluewater.client.connection.session;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.namehillsoftware.handoff.promises.Promise;

public interface HoldSessionConnection {
	Promise<HoldSessionConnection> setSessionConnection(IConnectionProvider connectionProvider);
	Promise<IConnectionProvider> promiseSessionConnection();
}
