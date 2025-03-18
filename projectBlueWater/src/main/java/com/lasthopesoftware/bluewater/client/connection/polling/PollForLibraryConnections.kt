package com.lasthopesoftware.bluewater.client.connection.polling

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.namehillsoftware.handoff.promises.Promise

interface PollForLibraryConnections {
	fun pollConnection(libraryId: LibraryId): Promise<LiveServerConnection>
}
