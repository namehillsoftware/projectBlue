package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.IBrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess
import com.namehillsoftware.handoff.promises.Promise

class LibraryRemoval(
	private val storedItems: IStoredItemAccess,
	private val libraryStorage: ILibraryStorage,
	private val selectedLibraryIdProvider: ISelectedLibraryIdentifierProvider,
	private val libraryProvider: ILibraryProvider,
	private val librarySelection: IBrowserLibrarySelection) : RemoveLibraries {

	override fun removeLibrary(library: Library): Promise<*> {
		val selectedLibraryId = selectedLibraryIdProvider.selectedLibraryId

		val promisedNewLibrarySelection =
			if (selectedLibraryId != library.libraryId) Promise.empty()
			else libraryProvider.allLibraries.eventually { libraries ->
				val firstOtherLibrary = libraries.firstOrNull { l -> l.libraryId != library.libraryId }
				if (firstOtherLibrary != null) librarySelection.selectBrowserLibrary(firstOtherLibrary.libraryId)
				else Promise.empty()
			}

		return promisedNewLibrarySelection.eventually {
			Promise.whenAll<Any>(
				storedItems.disableAllLibraryItems(library.libraryId) as Promise<Any>,
				libraryStorage.removeLibrary(library) as Promise<Any>)
		}
	}
}
