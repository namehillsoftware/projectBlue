package com.lasthopesoftware.bluewater.client.browsing.library.repository

import kotlinx.serialization.Serializable

@Serializable
data class StoredMediaCenterConnectionSettings(
	val accessCode: String? = null,
	val userName: String? = null,
	val password: String? = null,
	val isLocalOnly: Boolean = false,
	val isSyncLocalConnectionsOnly: Boolean = false,
	val syncedFileLocation: SyncedFileLocation? = null,
	val isWakeOnLanEnabled: Boolean = false,
	val sslCertificateFingerprint: String? = null,
	val macAddress: String? = null,
)
