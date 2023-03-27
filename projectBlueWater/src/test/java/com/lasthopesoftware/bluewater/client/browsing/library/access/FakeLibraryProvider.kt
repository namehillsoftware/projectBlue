package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class FakeLibraryProvider(vararg libraries: Library) : ILibraryProvider, ILibraryStorage {
	private val libraries = libraries.associateBy { l -> l.id }.toMutableMap()

    override fun promiseLibrary(libraryId: LibraryId): Promise<Library?> = Promise(libraries[libraryId.id])

    override val allLibraries: Promise<Collection<Library>>
        get() = Promise(libraries.values)

	override fun saveLibrary(library: Library): Promise<Library> =
		library.copy().also { libraries[it.id] = it }.toPromise()

	override fun removeLibrary(library: Library): Promise<Unit> {
		libraries.remove(library.id)
		return Unit.toPromise()
	}
}
