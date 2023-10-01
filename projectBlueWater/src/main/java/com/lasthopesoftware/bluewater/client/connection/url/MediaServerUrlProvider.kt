package com.lasthopesoftware.bluewater.client.connection.url

import com.lasthopesoftware.resources.uri.IoCommon
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLEncoder

class MediaServerUrlProvider constructor(
	override val authCode: String?,
	baseUrl: URL,
	override val certificateFingerprint: ByteArray
) : IUrlProvider {

	private val baseURL = URL(baseUrl, "/MCWS/v1/")

	constructor(authCode: String?, ipAddress: String?, port: Int)
		: this(authCode, URL(IoCommon.httpUriScheme, ipAddress, port, ""), ByteArray(0))

	constructor(authCode: String?, ipAddress: String?, port: Int, certificateFingerprint: ByteArray)
		: this(authCode, URL(IoCommon.httpsUriScheme, ipAddress, port, ""), certificateFingerprint)

	override val baseUrl: URL
		get() = baseURL

	override fun getUrl(vararg params: String): String { // Add base url
		val urlString = baseURL.toString()
		if (params.isEmpty()) return urlString

		val urlBuilder = StringBuilder(urlString)

		// Add action
		urlBuilder.append(params[0])

		if (params.size == 1) return urlBuilder.toString()

		// add arguments
		for (i in 1 until params.size) {
			urlBuilder.append(if (i == 1) '?' else '&')
			val param = params[i]
			val equalityIndex = param.indexOf('=')
			if (equalityIndex < 0) {
				urlBuilder.append(encodeParameter(param))
				continue
			}
			urlBuilder.append(encodeParameter(param.substring(0, equalityIndex))).append('=').append(encodeParameter(param.substring(equalityIndex + 1)))
		}

		return urlBuilder.toString()
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MediaServerUrlProvider

		if (authCode != other.authCode) return false
		if (!certificateFingerprint.contentEquals(other.certificateFingerprint)) return false
		if (baseURL != other.baseURL) return false

		return true
	}

	override fun hashCode(): Int {
		var result = authCode?.hashCode() ?: 0
		result = 31 * result + certificateFingerprint.contentHashCode()
		result = 31 * result + baseURL.hashCode()
		return result
	}

	companion object {
		private fun encodeParameter(parameter: String): String {
			return try {
				URLEncoder.encode(parameter, "UTF-8").replace("+", "%20")
			} catch (e: UnsupportedEncodingException) {
				parameter
			}
		}
	}

}
