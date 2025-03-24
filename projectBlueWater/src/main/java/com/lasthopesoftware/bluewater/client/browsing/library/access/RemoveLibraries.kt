package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface RemoveLibraries {
	fun removeLibrary(libraryId: LibraryId): Promise<*>
}
