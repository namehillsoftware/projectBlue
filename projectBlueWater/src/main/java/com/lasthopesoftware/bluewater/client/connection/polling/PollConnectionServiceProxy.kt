package com.lasthopesoftware.bluewater.client.connection.polling

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.LiveServerConnection
import com.namehillsoftware.handoff.promises.Promise

class PollConnectionServiceProxy(private val context: Context) : PollForLibraryConnections {
	override fun pollConnection(libraryId: LibraryId): Promise<LiveServerConnection> =
		PollConnectionService.pollSessionConnection(context, libraryId)
}
