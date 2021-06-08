package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class ConnectionSettingsLookup(private val libraryProvider: ILibraryProvider) : LookupConnectionSettings {


	override fun lookupConnectionSettings(libraryId: LibraryId): Promise<ConnectionSettings?> =
		libraryProvider.getLibrary(libraryId).then {
			it?.let {
				ConnectionSettings(
					it.accessCode,
					it.userName,
					it.password,
					it.isLocalOnly,
					it.isWakeOnLanEnabled)
			}
		}
}
