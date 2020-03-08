package com.lasthopesoftware.bluewater.client.connection.waking

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface WakeLibraryServer {
	fun awakeLibraryServer(libraryId: LibraryId): Promise<Unit>
}
