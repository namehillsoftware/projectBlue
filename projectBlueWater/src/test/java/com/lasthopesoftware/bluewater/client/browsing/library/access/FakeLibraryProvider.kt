package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class FakeLibraryProvider(vararg libraries: Library) : ILibraryProvider {
	private val libraries = libraries.toList()

    override fun getLibrary(libraryId: LibraryId): Promise<Library?> {
        return Promise(libraries.firstOrNull { l -> l.libraryId == libraryId })
    }

    override val allLibraries: Promise<Collection<Library>>
        get() = Promise(libraries)
}
