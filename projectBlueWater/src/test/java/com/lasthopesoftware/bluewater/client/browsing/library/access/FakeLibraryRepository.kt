package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryNowPlayingValues
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

open class FakeLibraryRepository(vararg libraries: Library) : ProvideLibraries, ManageLibraries {
	val libraries = ConcurrentHashMap(libraries.associateBy { l -> l.id })

    override fun promiseLibrary(libraryId: LibraryId): Promise<Library?> = Promise(libraries[libraryId.id])

	override fun promiseNowPlayingValues(libraryId: LibraryId): Promise<LibraryNowPlayingValues?> =
		libraries[libraryId.id]
			?.run { LibraryNowPlayingValues(id, isRepeating, nowPlayingId, nowPlayingProgress, savedTracksString) }
			.toPromise()

	override fun promiseAllLibraries(): Promise<Collection<Library>> = Promise(libraries.values)

	override fun saveLibrary(library: Library): Promise<Library> =
		library.copy().also { libraries[it.id] = it }.toPromise()

	override fun updateNowPlaying(values: LibraryNowPlayingValues): Promise<Unit> {
		val library = libraries[values.id] ?: return Promise.empty()
		with (values) {
			library.copy(
				nowPlayingId = nowPlayingId,
				nowPlayingProgress = nowPlayingProgress,
				savedTracksString = savedTracksString,
				isRepeating = isRepeating
			).also { libraries[it.id] = it }
		}

		return Unit.toPromise()
	}

	override fun removeLibrary(libraryId: LibraryId): Promise<Unit> {
		libraries.remove(libraryId.id)
		return Unit.toPromise()
	}
}
