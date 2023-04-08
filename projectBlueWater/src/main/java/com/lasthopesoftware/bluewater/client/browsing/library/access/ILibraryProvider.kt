package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ILibraryProvider {
	fun promiseLibrary(libraryId: LibraryId): Promise<Library?>
	val allLibraries: Promise<Collection<Library>>
}
