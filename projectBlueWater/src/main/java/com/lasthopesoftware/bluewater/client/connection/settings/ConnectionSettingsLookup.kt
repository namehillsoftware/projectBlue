package com.lasthopesoftware.bluewater.client.connection.settings

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.parsedConnectionSettings
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise

class ConnectionSettingsLookup(private val libraryProvider: ILibraryProvider) : LookupConnectionSettings {
	@OptIn(ExperimentalStdlibApi::class)
	override fun lookupConnectionSettings(libraryId: LibraryId): Promise<MediaCenterConnectionSettings?> =
		libraryProvider
			.promiseLibrary(libraryId)
			.then { it ->
				it?.parsedConnectionSettings()?.run {
					MediaCenterConnectionSettings(
						accessCode = accessCode ?: throw MissingAccessCodeException(libraryId),
						userName = userName,
						password = password,
						isLocalOnly = isLocalOnly,
						isWakeOnLanEnabled = isWakeOnLanEnabled,
						sslCertificateFingerprint = sslCertificateFingerprint?.hexToByteArray() ?: emptyByteArray,
					)
				}
			}
}
