package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

interface ProvideSessionConnection {
	fun promiseSessionConnection(): Promise<IConnectionProvider?>
}
