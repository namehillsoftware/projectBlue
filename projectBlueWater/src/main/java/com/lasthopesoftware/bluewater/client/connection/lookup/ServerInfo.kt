package com.lasthopesoftware.bluewater.client.connection.lookup

import com.lasthopesoftware.resources.emptyByteArray

data class ServerInfo(
	val httpPort: Int? = null,
	val httpsPort: Int? = null,
	val remoteHosts: Set<String> = emptySet(),
	val localHosts: Set<String> = emptySet(),
	val macAddresses: Set<String> = emptySet(),
	val certificateFingerprint: ByteArray = emptyByteArray
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ServerInfo

		if (httpPort != other.httpPort) return false
		if (httpsPort != other.httpsPort) return false
		if (remoteHosts != other.remoteHosts) return false
		if (localHosts != other.localHosts) return false
		if (macAddresses != other.macAddresses) return false
		if (!certificateFingerprint.contentEquals(other.certificateFingerprint)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = httpPort ?: 0
		result = 31 * result + (httpsPort ?: 0)
		result = 31 * result + remoteHosts.hashCode()
		result = 31 * result + localHosts.hashCode()
		result = 31 * result + macAddresses.hashCode()
		result = 31 * result + certificateFingerprint.contentHashCode()
		return result
	}
}
