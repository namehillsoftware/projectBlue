package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLibraryConnections {
	fun promiseLibraryConnection(libraryId: LibraryId): Promise<LiveServerConnection?>
}

