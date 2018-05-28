package com.lasthopesoftware.bluewater.client.connection.testing;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.namehillsoftware.handoff.promises.Promise;

public interface TestConnections {
	Promise<Boolean> promiseIsConnectionPossible(ConnectionProvider connectionProvider);
}
