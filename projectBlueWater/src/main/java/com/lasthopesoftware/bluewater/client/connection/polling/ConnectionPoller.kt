package com.lasthopesoftware.bluewater.client.connection.polling

import android.content.Context
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

class ConnectionPoller(private val context: Context) : PollForConnections {
	override fun pollSessionConnection(withNotification: Boolean): Promise<IConnectionProvider> =
		PollConnectionService.pollSessionConnection(context, withNotification)
}
