package com.lasthopesoftware.bluewater.client.connection.url

import com.lasthopesoftware.resources.emptyByteArray
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLEncoder

class AudiusUrlProvider(baseUrl: URL) : ProvideUrls {
	override val authCode = ""

	override val certificateFingerprint = emptyByteArray

	override val baseUrl = URL(baseUrl, "/v1/")

	override fun getUrl(vararg params: String): String { // Add base url
		val urlString = baseUrl.toString()
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

		other as AudiusUrlProvider

		if (authCode != other.authCode) return false
		if (!certificateFingerprint.contentEquals(other.certificateFingerprint)) return false
		if (baseUrl != other.baseUrl) return false

		return true
	}

	override fun hashCode(): Int {
		var result = authCode.hashCode()
		result = 31 * result + certificateFingerprint.contentHashCode()
		result = 31 * result + baseUrl.hashCode()
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
