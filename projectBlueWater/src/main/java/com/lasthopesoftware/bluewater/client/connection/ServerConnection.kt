package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.uri.IoCommon
import java.net.URL

data class ServerConnection(
	val baseUrl: URL,
	val authCode: String? = null,
	val certificateFingerprint: ByteArray = emptyByteArray,
) {
	constructor(authCode: String?, ipAddress: String?, port: Int)
		: this(URL(IoCommon.httpUriScheme, ipAddress, port, ""), authCode)

	constructor(authCode: String?, ipAddress: String?, port: Int, certificateFingerprint: ByteArray)
		: this(URL(IoCommon.httpsUriScheme, ipAddress, port, ""), authCode, certificateFingerprint)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ServerConnection

		if (baseUrl != other.baseUrl) return false
		if (authCode != other.authCode) return false
		if (!certificateFingerprint.contentEquals(other.certificateFingerprint)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = baseUrl.hashCode()
		result = 31 * result + (authCode?.hashCode() ?: 0)
		result = 31 * result + (certificateFingerprint.contentHashCode() ?: 0)
		return result
	}
}
