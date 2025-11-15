package com.lasthopesoftware.bluewater.client.browsing.library.settings

import androidx.annotation.Keep

@Keep
data class StoredMediaCenterConnectionSettings(
	val accessCode: String? = null,
	val userName: String? = null,
	override val password: String? = null,
	val isLocalOnly: Boolean = false,
	val isSyncLocalConnectionsOnly: Boolean = false,
	val isWakeOnLanEnabled: Boolean = false,
	val sslCertificateFingerprint: String? = null,
	val macAddress: String? = null,
) : PasswordStoredConnectionSettings
