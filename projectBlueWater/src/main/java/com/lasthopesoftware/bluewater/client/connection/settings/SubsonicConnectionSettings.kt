package com.lasthopesoftware.bluewater.client.connection.settings

import androidx.annotation.Keep
import com.lasthopesoftware.resources.emptyByteArray

@Keep
data class SubsonicConnectionSettings(
	val url: String,
	val userName: String,
	val password: String,
	val isWakeOnLanEnabled: Boolean = false,
	val sslCertificateFingerprint: ByteArray = emptyByteArray,
	val macAddress: String? = null,
) : ConnectionSettings {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as SubsonicConnectionSettings

		if (isWakeOnLanEnabled != other.isWakeOnLanEnabled) return false
		if (url != other.url) return false
		if (userName != other.userName) return false
		if (password != other.password) return false
		if (!sslCertificateFingerprint.contentEquals(other.sslCertificateFingerprint)) return false
		if (macAddress != other.macAddress) return false

		return true
	}

	override fun hashCode(): Int {
		var result = isWakeOnLanEnabled.hashCode()
		result = 31 * result + url.hashCode()
		result = 31 * result + userName.hashCode()
		result = 31 * result + password.hashCode()
		result = 31 * result + sslCertificateFingerprint.contentHashCode()
		result = 31 * result + (macAddress?.hashCode() ?: 0)
		return result
	}
}
