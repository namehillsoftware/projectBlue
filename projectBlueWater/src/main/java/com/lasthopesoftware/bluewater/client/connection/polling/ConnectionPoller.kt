package com.lasthopesoftware.bluewater.client.connection.polling

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

class ConnectionPoller(private val context: Context) : PollForConnections {
	override fun pollConnection(libraryId: LibraryId, withNotification: Boolean): Promise<IConnectionProvider> =
		PollConnectionService.pollSessionConnection(context, libraryId, withNotification)
}
