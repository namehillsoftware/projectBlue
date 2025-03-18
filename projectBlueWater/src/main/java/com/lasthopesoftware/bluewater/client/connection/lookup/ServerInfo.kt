package com.lasthopesoftware.bluewater.client.connection.lookup

import com.lasthopesoftware.resources.emptyByteArray

data class ServerInfo(
	val httpPort: Int? = null,
	val httpsPort: Int? = null,
	val remoteHost: String? = null,
	val localIps: Set<String> = emptySet(),
	val macAddresses: Set<String> = emptySet(),
	val certificateFingerprint: ByteArray = emptyByteArray
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ServerInfo

		if (httpPort != other.httpPort) return false
		if (httpsPort != other.httpsPort) return false
		if (remoteHost != other.remoteHost) return false
		if (localIps != other.localIps) return false
		if (macAddresses != other.macAddresses) return false
		if (!certificateFingerprint.contentEquals(other.certificateFingerprint)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = httpPort ?: 0
		result = 31 * result + (httpsPort ?: 0)
		result = 31 * result + (remoteHost?.hashCode() ?: 0)
		result = 31 * result + localIps.hashCode()
		result = 31 * result + macAddresses.hashCode()
		result = 31 * result + certificateFingerprint.contentHashCode()
		return result
	}
}
