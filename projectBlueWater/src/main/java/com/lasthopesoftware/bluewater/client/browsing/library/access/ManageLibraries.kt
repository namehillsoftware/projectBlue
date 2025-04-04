package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryNowPlayingValues
import com.namehillsoftware.handoff.promises.Promise

interface ManageLibraries : ProvideLibraries {
	fun saveLibrary(library: Library): Promise<Library>

	fun updateNowPlaying(values: LibraryNowPlayingValues): Promise<Unit>

	fun removeLibrary(libraryId: LibraryId): Promise<Unit>
}
