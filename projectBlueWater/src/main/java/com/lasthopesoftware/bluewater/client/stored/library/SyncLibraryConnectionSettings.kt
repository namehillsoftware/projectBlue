package com.lasthopesoftware.bluewater.client.stored.library

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.ProvideLibrarySettings
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupValidConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MissingAccessCodeException
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise

class SyncLibraryConnectionSettings(private val librarySettings: ProvideLibrarySettings) : LookupValidConnectionSettings {
	@OptIn(ExperimentalStdlibApi::class)
	override fun promiseConnectionSettings(libraryId: LibraryId): Promise<ConnectionSettings?> =
		librarySettings
			.promiseLibrarySettings(libraryId)
			.then { it ->
				it?.connectionSettings?.run {
					if (this is StoredMediaCenterConnectionSettings) {
						MediaCenterConnectionSettings(
							accessCode = accessCode ?: throw MissingAccessCodeException(libraryId),
							userName = userName,
							password = password,
							isLocalOnly = isLocalOnly || isSyncLocalConnectionsOnly,
							isWakeOnLanEnabled = isWakeOnLanEnabled,
							sslCertificateFingerprint = sslCertificateFingerprint?.hexToByteArray() ?: emptyByteArray,
						)
					} else null
				}
			}
}
