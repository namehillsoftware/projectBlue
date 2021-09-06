package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectBrowserLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess
import com.namehillsoftware.handoff.promises.Promise

class LibraryRemoval(
	private val storedItems: IStoredItemAccess,
	private val libraryStorage: ILibraryStorage,
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val libraryProvider: ILibraryProvider,
	private val librarySelection: SelectBrowserLibrary) : RemoveLibraries {

	override fun removeLibrary(library: Library): Promise<*> {
		val promisedNewLibrarySelection =
			selectedLibraryIdProvider.selectedLibraryId
				.eventually {
					if (library.libraryId != it) Promise.empty()
					else libraryProvider.allLibraries.eventually { libraries ->
						val firstOtherLibrary = libraries.firstOrNull { l -> l.libraryId != library.libraryId }
						if (firstOtherLibrary != null) librarySelection.selectBrowserLibrary(firstOtherLibrary.libraryId)
						else Promise.empty()
					}
				}

		return promisedNewLibrarySelection.eventually {
			Promise.whenAll(
				storedItems.disableAllLibraryItems(library.libraryId),
				libraryStorage.removeLibrary(library))
		}
	}
}
