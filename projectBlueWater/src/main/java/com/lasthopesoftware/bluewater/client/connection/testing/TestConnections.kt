package com.lasthopesoftware.bluewater.client.connection.testing

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

interface TestConnections {
	fun promiseIsConnectionPossible(connectionProvider: IConnectionProvider): Promise<Boolean>
}
