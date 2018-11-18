package com.lasthopesoftware.bluewater.client.connection.testing;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.namehillsoftware.handoff.promises.Promise;
import org.joda.time.Duration;

public interface TestConnections {
	Promise<Boolean> promiseIsConnectionPossible(IConnectionProvider connectionProvider);

	Promise<Boolean> promiseIsConnectionPossible(IConnectionProvider connectionProvider, Duration timeout);
}
