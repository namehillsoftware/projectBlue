package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.uri.IoCommon
import java.net.URL

data class MediaCenterConnectionDetails(
	val baseUrl: URL,
	val authCode: String? = null,
	val certificateFingerprint: ByteArray = emptyByteArray,
	val customHeaders: Map<String, String> = emptyMap(),
) {
	constructor(authCode: String?, ipAddress: String?, port: Int, customHeaders: Map<String, String> = emptyMap())
		: this(baseUrl = URL(IoCommon.httpUriScheme, ipAddress, port, ""), authCode = authCode, customHeaders = customHeaders)

	constructor(authCode: String?, ipAddress: String?, port: Int, certificateFingerprint: ByteArray, customHeaders: Map<String, String> = emptyMap())
		: this(URL(IoCommon.httpsUriScheme, ipAddress, port, ""), authCode, certificateFingerprint, customHeaders)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MediaCenterConnectionDetails

		if (baseUrl != other.baseUrl) return false
		if (authCode != other.authCode) return false
		if (!certificateFingerprint.contentEquals(other.certificateFingerprint)) return false
		if (customHeaders != other.customHeaders) return false

		return true
	}

	override fun hashCode(): Int {
		var result = baseUrl.hashCode()
		result = 31 * result + (authCode?.hashCode() ?: 0)
		result = 31 * result + certificateFingerprint.contentHashCode()
		result = 31 * result + customHeaders.hashCode()
		return result
	}
}
