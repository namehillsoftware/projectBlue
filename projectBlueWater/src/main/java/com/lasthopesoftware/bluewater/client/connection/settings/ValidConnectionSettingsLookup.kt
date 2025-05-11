package com.lasthopesoftware.bluewater.client.connection.settings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class ValidConnectionSettingsLookup(private val connectionSettings: LookupConnectionSettings) : LookupValidConnectionSettings {
	override fun promiseConnectionSettings(libraryId: LibraryId): Promise<ConnectionSettings?> =
		connectionSettings
			.promiseConnectionSettings(libraryId)
			.then { it ->
				it?.also {
					when (it) {
						is MediaCenterConnectionSettings -> {
							if (it.accessCode.isBlank()) throw MissingAccessCodeException(libraryId)
						}

						is SubsonicConnectionSettings -> {
							if (it.url.isBlank()) throw MissingAccessCodeException(libraryId)
							if (it.userName.isBlank()) throw MissingAccessCodeException(libraryId)
							if (it.password.isBlank()) throw MissingAccessCodeException(libraryId)
						}
					}
				}
			}
}
