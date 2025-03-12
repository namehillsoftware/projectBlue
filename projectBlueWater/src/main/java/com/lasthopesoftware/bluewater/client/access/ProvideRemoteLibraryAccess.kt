package com.lasthopesoftware.bluewater.client.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideRemoteLibraryAccess {
	fun promiseLibraryAccess(libraryId: LibraryId): Promise<RemoteLibraryAccess?>
}
