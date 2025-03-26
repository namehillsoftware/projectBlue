package com.lasthopesoftware.bluewater.client.browsing.library.settings

import androidx.annotation.Keep

@Keep
data class StoredSubsonicConnectionSettings(
	val url: String? = null,
    val userName: String? = null,
    val password: String? = null,
	val isWakeOnLanEnabled: Boolean = false,
	val sslCertificateFingerprint: String? = null,
	val macAddress: String? = null,
) : StoredConnectionSettings
