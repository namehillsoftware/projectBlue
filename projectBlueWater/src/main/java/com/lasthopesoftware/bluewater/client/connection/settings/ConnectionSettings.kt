package com.lasthopesoftware.bluewater.client.connection.settings

data class ConnectionSettings(
	val accessCode: String,
	val userName: String? = null,
	val password: String? = null,
	val isLocalOnly: Boolean = false,
	val isWakeOnLanEnabled: Boolean = false,
	val sslCertificateFingerprint: ByteArray = ByteArray(0),
	val macAddress: String? = null,
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ConnectionSettings

		if (accessCode != other.accessCode) return false
		if (userName != other.userName) return false
		if (password != other.password) return false
		if (isLocalOnly != other.isLocalOnly) return false
		if (isWakeOnLanEnabled != other.isWakeOnLanEnabled) return false
		if (!sslCertificateFingerprint.contentEquals(other.sslCertificateFingerprint)) return false
		if (macAddress != other.macAddress) return false

		return true
	}

	override fun hashCode(): Int {
		var result = accessCode.hashCode()
		result = 31 * result + (userName?.hashCode() ?: 0)
		result = 31 * result + (password?.hashCode() ?: 0)
		result = 31 * result + isLocalOnly.hashCode()
		result = 31 * result + isWakeOnLanEnabled.hashCode()
		result = 31 * result + sslCertificateFingerprint.contentHashCode()
		result = 31 * result + (macAddress?.hashCode() ?: 0)
		return result
	}

	fun isUserCredentialsValid(): Boolean =
		!userName.isNullOrEmpty() && !password.isNullOrEmpty()
}
