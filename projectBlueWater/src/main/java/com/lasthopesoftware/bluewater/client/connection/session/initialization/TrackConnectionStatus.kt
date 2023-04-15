package com.lasthopesoftware.bluewater.client.connection.session.initialization

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface TrackConnectionStatus {
	fun initializeConnection(libraryId: LibraryId): Promise<Boolean>
}
