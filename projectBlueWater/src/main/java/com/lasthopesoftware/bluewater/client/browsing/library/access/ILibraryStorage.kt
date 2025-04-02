package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

/**
 * Created by david on 2/11/17.
 */
interface ILibraryStorage {
	fun saveLibrary(library: Library): Promise<Library>

	fun updateNowPlaying(libraryId: LibraryId, nowPlayingId: Int, nowPlayingProgress: Long, savedTracksString: String, isRepeating: Boolean): Promise<Unit>

	fun removeLibrary(libraryId: LibraryId): Promise<Unit>
}
