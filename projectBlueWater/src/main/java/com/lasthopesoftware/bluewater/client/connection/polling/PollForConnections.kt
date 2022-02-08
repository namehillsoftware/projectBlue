package com.lasthopesoftware.bluewater.client.connection.polling

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

interface PollForConnections {
	fun pollSessionConnection(withNotification: Boolean = false): Promise<IConnectionProvider>
}
