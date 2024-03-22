package com.lasthopesoftware.bluewater.client.connection.testing

import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.namehillsoftware.handoff.promises.Promise

interface TestConnections {
	fun promiseIsConnectionPossible(connectionProvider: ProvideConnections): Promise<Boolean>
}
