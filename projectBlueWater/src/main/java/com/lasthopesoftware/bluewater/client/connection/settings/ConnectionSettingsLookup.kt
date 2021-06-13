package com.lasthopesoftware.bluewater.client.connection.settings

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class ConnectionSettingsLookup(private val libraryProvider: ILibraryProvider) : LookupConnectionSettings {


	override fun lookupConnectionSettings(libraryId: LibraryId): Promise<ConnectionSettings?> =
		libraryProvider.getLibrary(libraryId).then {
			it?.run {
				ConnectionSettings(
					accessCode,
					userName,
					password,
					isLocalOnly,
					isWakeOnLanEnabled)
			}
		}
}
