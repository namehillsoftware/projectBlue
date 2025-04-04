package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectBrowserLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class LibraryRemoval(
	private val storedItems: AccessStoredItems,
	private val libraryStorage: ManageLibraries,
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val libraryProvider: ProvideLibraries,
	private val librarySelection: SelectBrowserLibrary) : RemoveLibraries {

	override fun removeLibrary(libraryId: LibraryId): Promise<*> {
		val promisedNewLibrarySelection = selectedLibraryIdProvider
			.promiseSelectedLibraryId()
			.eventually {
				if (libraryId != it) Promise.empty()
				else libraryProvider.promiseAllLibraries().eventually { libraries ->
					libraries
						.firstOrNull { l -> l.libraryId != libraryId }
						?.let { firstOtherLibrary -> librarySelection.selectBrowserLibrary(firstOtherLibrary.libraryId) }
						.keepPromise()
				}
			}

		return promisedNewLibrarySelection.eventually {
			Promise.whenAll(
				storedItems.disableAllLibraryItems(libraryId),
				libraryStorage.removeLibrary(libraryId))
		}
	}
}
