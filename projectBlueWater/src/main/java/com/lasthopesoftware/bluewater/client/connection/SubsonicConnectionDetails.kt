package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.resources.emptyByteArray
import java.net.URL

data class SubsonicConnectionDetails(
	val baseUrl: URL,
	val userName: String,
	val password: String,
	val salt: String = "",
	val certificateFingerprint: ByteArray = emptyByteArray,
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as SubsonicConnectionDetails

		if (baseUrl != other.baseUrl) return false
		if (userName != other.userName) return false
		if (password != other.password) return false
		if (salt != other.salt) return false
		if (!certificateFingerprint.contentEquals(other.certificateFingerprint)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = baseUrl.hashCode()
		result = 31 * result + userName.hashCode()
		result = 31 * result + password.hashCode()
		result = 31 * result + salt.hashCode()
		result = 31 * result + certificateFingerprint.contentHashCode()
		return result
	}
}
