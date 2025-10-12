package com.lasthopesoftware.bluewater.client.connection.requests

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.namehillsoftware.handoff.promises.Promise
import java.io.IOException
import java.net.URL

class FakeHttpConnection(
	vararg responses: Pair<URL, (URL) -> PassThroughHttpResponse>
) : HttpPromiseClient {
	private val requests = ArrayList<URL>()

	private val mappedResponses = HashMap<UrlParts, (URL) -> PassThroughHttpResponse>()

	val recordedRequests: List<URL>
		get() = requests

	init {
	    for (response in responses) {
			mapResponse(response.first, response.second)
		}
	}

	fun setupFile(url: URL, serviceFile: ServiceFile, fileProperties: Map<String, String>) {
		mapResponse(
			url,
			{
				val returnXml = StringBuilder(
					"""<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
<MPL Version="2.0" Title="MCWS - Files - 10936" PathSeparator="\">
<Item>
<Field Name="Key">${serviceFile.key}</Field>
<Field Name="Media Type">Audio</Field>
"""
				)
				for ((key, value) in fileProperties) returnXml.append("<Field Name=\"").append(key)
					.append("\">").append(
						value
					).append("</Field>\n")
				returnXml.append(
					"""
    </Item>
    </MPL>

    """.trimIndent()
				)

				PassThroughHttpResponse(
					200,
					"test",
					returnXml.toString().toByteArray().inputStream()
				)
			},
		)
	}

	fun mapResponse(url: URL, response: (URL) -> PassThroughHttpResponse) {
		mappedResponses[UrlParts(url)] = response
	}

	override fun promiseResponse(url: URL): Promise<HttpResponse> {
		requests.add(url)

		return try {
			Promise(getResponse(url))
		} catch (e: IOException) {
			Promise(e)
		} catch (e: RuntimeException) {
			Promise(e.cause)
		}
	}

	override fun promiseResponse(method: String, headers: Map<String, String>, url: URL): Promise<HttpResponse> =
		promiseResponse(url)

	private fun getResponse(url: URL): HttpResponse {
		val mappedResponse = mappedResponses[UrlParts(url)]
			?: return PassThroughHttpResponse(
				code = 404,
				message = "Not Found",
				"Not found".toByteArray().inputStream()
			)
		try {
			val result = mappedResponse(url)
			return result
		} catch (io: IOException) {
			throw io
		} catch (error: Throwable) {
			throw RuntimeException(error)
		}
	}

	private data class UrlParts(
		val scheme: String,
		val host: String,
		val port: Int,
		val path: String = "",
		val query: Map<String, String> = emptyMap(),
		val fragment: String? = null
	) {
		constructor(url: URL) : this(
			url.protocol,
			url.host,
			url.port,
			url.path,
			url.query
				?.split("&")
				?.associate {
					it.split("=").let { kv -> kv[0] to kv.getOrElse(1, { "" }) }
				}
				?: emptyMap(),
			url.ref
		)
	}
}
