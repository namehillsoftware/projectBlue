package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryNowPlayingValues
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLibraries {
	fun promiseLibrary(libraryId: LibraryId): Promise<Library?>
	fun promiseNowPlayingValues(libraryId: LibraryId): Promise<LibraryNowPlayingValues?>
	fun promiseAllLibraries(): Promise<Collection<Library>>
}
