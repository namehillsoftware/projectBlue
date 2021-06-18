package com.lasthopesoftware.bluewater.client.connection.settings.changes

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.namehillsoftware.handoff.promises.Promise

class ConnectionSettingsChangeDetectionLibraryStorage(private val inner: ILibraryStorage, private val connectionSettingsLookup: LookupConnectionSettings) : ILibraryStorage {
	override fun saveLibrary(library: Library): Promise<Library> {
		/*return connectionSettingsLookup.lookupConnectionSettings(library.libraryId)
			.eventually { originalConnectionSettings ->

			}*/

		return inner.saveLibrary(library)
	}

	override fun removeLibrary(library: Library): Promise<Unit> {
		TODO("Not yet implemented")
	}
}
