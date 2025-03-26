package com.lasthopesoftware.bluewater.client.connection.settings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class ValidConnectionSettingsLookup(private val connectionSettings: LookupConnectionSettings) : LookupValidConnectionSettings {
	override fun promiseConnectionSettings(libraryId: LibraryId): Promise<ConnectionSettings?> =
		connectionSettings
			.promiseConnectionSettings(libraryId)
			.then { it ->
				it?.also {
					if (it is MediaCenterConnectionSettings && it.accessCode.isBlank()) throw MissingAccessCodeException(libraryId)
				}
			}
}
