package com.lasthopesoftware.bluewater.client.connection.testing;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.namehillsoftware.handoff.promises.Promise;

public interface TestConnections {
	Promise<Boolean> promiseIsConnectionPossible(IConnectionProvider connectionProvider);
}
