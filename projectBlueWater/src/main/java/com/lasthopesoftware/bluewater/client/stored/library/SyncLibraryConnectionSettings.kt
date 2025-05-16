package com.lasthopesoftware.bluewater.client.stored.library

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredSubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.ProvideLibrarySettings
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupValidConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MissingAccessCodeException
import com.lasthopesoftware.bluewater.client.connection.settings.SubsonicConnectionSettings
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise

class SyncLibraryConnectionSettings(private val librarySettings: ProvideLibrarySettings) : LookupValidConnectionSettings {
	@OptIn(ExperimentalStdlibApi::class)
	override fun promiseConnectionSettings(libraryId: LibraryId): Promise<ConnectionSettings?> =
		librarySettings
			.promiseLibrarySettings(libraryId)
			.then { it ->
				it?.connectionSettings?.let {
					when (it) {
						is StoredMediaCenterConnectionSettings -> MediaCenterConnectionSettings(
							accessCode = it.accessCode ?: throw MissingAccessCodeException(libraryId),
							userName = it.userName,
							password = it.password,
							isLocalOnly = it.isLocalOnly || it.isSyncLocalConnectionsOnly,
							isWakeOnLanEnabled = it.isWakeOnLanEnabled,
							macAddress = it.macAddress,
							sslCertificateFingerprint = it.sslCertificateFingerprint?.hexToByteArray() ?: emptyByteArray,
						)
						is StoredSubsonicConnectionSettings -> SubsonicConnectionSettings(
							url = it.url ?: throw MissingAccessCodeException(libraryId),
							password = it.password ?: throw MissingAccessCodeException(libraryId),
							userName = it.userName ?: throw MissingAccessCodeException(libraryId),
							sslCertificateFingerprint = it.sslCertificateFingerprint?.hexToByteArray() ?: emptyByteArray,
							macAddress = it.macAddress,
							isWakeOnLanEnabled = it.isWakeOnLanEnabled,
						)
					}
				}
			}
}
