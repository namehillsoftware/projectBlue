package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

interface ProvideSelectedConnection {
	fun promiseSessionConnection(): Promise<IConnectionProvider?>
}
