package com.lasthopesoftware.bluewater.client.connection.url

import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLEncoder

object UrlBuilder {
	fun URL.withMcApi() = addPath("/MCWS/v1/")

	fun URL.addPath(path: String) = URL(this, path)

	fun URL.addParams(vararg params: String): URL {
		val urlBuilder = StringBuilder(toString())

		val currentQuery = query

		// add arguments
		for (i in params.indices) {
			urlBuilder.append(if (i == 0 && currentQuery == null) '?' else '&')
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

	private fun encodeParameter(parameter: String): String = try {
		URLEncoder.encode(parameter, "UTF-8").replace("+", "%20")
	} catch (e: UnsupportedEncodingException) {
		parameter
	}
}
