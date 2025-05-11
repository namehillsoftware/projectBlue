package com.lasthopesoftware.bluewater.client.connection.settings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredSubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.ProvideLibrarySettings
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise

class ConnectionSettingsLookup(private val librarySettings: ProvideLibrarySettings) : LookupConnectionSettings {
	@OptIn(ExperimentalStdlibApi::class)
	override fun promiseConnectionSettings(libraryId: LibraryId): Promise<ConnectionSettings?> =
		librarySettings
			.promiseLibrarySettings(libraryId)
			.then { it ->
				it?.connectionSettings?.run {
					when (this) {
						is StoredMediaCenterConnectionSettings -> MediaCenterConnectionSettings(
							accessCode = accessCode ?: "",
							userName = userName,
							password = password,
							isLocalOnly = isLocalOnly,
							isWakeOnLanEnabled = isWakeOnLanEnabled,
							sslCertificateFingerprint = sslCertificateFingerprint?.hexToByteArray() ?: emptyByteArray,
							macAddress = macAddress)
						is StoredSubsonicConnectionSettings -> SubsonicConnectionSettings(
							url = url ?: "",
							userName = userName ?: "",
							password = password ?: "",
							isWakeOnLanEnabled = isWakeOnLanEnabled,
							sslCertificateFingerprint = sslCertificateFingerprint?.hexToByteArray() ?: emptyByteArray,
							macAddress = macAddress,
						)
					}
				}
			}
}
