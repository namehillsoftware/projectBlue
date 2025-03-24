package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

open class FakeLibraryRepository(vararg libraries: Library) : ILibraryProvider, ILibraryStorage {
	val libraries = libraries.associateBy { l -> l.id }.toMutableMap()

    override fun promiseLibrary(libraryId: LibraryId): Promise<Library?> = Promise(libraries[libraryId.id])

    override fun promiseAllLibraries(): Promise<Collection<Library>> = Promise(libraries.values)

	override fun saveLibrary(library: Library): Promise<Library> =
		library.copy().also { libraries[it.id] = it }.toPromise()

	override fun removeLibrary(libraryId: LibraryId): Promise<Unit> {
		libraries.remove(libraryId.id)
		return Unit.toPromise()
	}
}
