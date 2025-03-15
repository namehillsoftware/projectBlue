package com.lasthopesoftware.bluewater.client.connection.url

import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLEncoder

object JRiverUrlBuilder {
	fun getUrl(baseUrl: URL, path: String, vararg params: String): URL {
		// Add action
		val urlWithPath = URL(baseUrl, path)

		if (params.isEmpty()) return urlWithPath

		val urlBuilder = StringBuilder(urlWithPath.toString())

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

		return URL(urlBuilder.toString())
	}

	private fun encodeParameter(parameter: String): String {
		return try {
			URLEncoder.encode(parameter, "UTF-8").replace("+", "%20")
		} catch (e: UnsupportedEncodingException) {
			parameter
		}
	}
}
