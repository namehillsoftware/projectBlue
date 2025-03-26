package com.lasthopesoftware.bluewater.client.connection.settings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.ProvideLibrarySettings
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise

class ConnectionSettingsLookup(private val librarySettings: ProvideLibrarySettings) : LookupValidConnectionSettings {
	@OptIn(ExperimentalStdlibApi::class)
	override fun promiseConnectionSettings(libraryId: LibraryId): Promise<MediaCenterConnectionSettings?> =
		librarySettings
			.promiseLibrarySettings(libraryId)
			.then { it ->
				it?.connectionSettings?.run {
					MediaCenterConnectionSettings(
						accessCode = accessCode ?: "",
						userName = userName,
						password = password,
						isLocalOnly = isLocalOnly,
						isWakeOnLanEnabled = isWakeOnLanEnabled,
						sslCertificateFingerprint = sslCertificateFingerprint?.hexToByteArray() ?: emptyByteArray,
					)
				}
			}
}
