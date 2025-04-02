package com.lasthopesoftware.bluewater.client.connection.settings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class ValidConnectionSettingsLookup(private val connectionSettings: LookupConnectionSettings) : LookupValidConnectionSettings {
	@OptIn(ExperimentalStdlibApi::class)
	override fun promiseConnectionSettings(libraryId: LibraryId): Promise<MediaCenterConnectionSettings?> =
		connectionSettings
			.promiseConnectionSettings(libraryId)
			.then { it ->
				it?.apply {
					if (accessCode.isBlank()) throw MissingAccessCodeException(libraryId)
				}
			}
}
