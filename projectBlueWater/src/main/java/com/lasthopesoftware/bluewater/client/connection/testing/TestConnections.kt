package com.lasthopesoftware.bluewater.client.connection.testing

import com.lasthopesoftware.bluewater.client.connection.LiveServerConnection
import com.namehillsoftware.handoff.promises.Promise

interface TestConnections {
	fun promiseIsConnectionPossible(connectionProvider: LiveServerConnection): Promise<Boolean>
}
