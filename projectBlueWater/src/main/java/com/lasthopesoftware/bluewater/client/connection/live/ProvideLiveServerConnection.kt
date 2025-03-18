package com.lasthopesoftware.bluewater.client.connection.live

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLiveServerConnection {
	fun promiseLiveServerConnection(libraryId: LibraryId): Promise<LiveServerConnection?>
}
