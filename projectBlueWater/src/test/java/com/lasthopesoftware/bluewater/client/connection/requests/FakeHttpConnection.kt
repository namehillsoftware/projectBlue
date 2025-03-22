package com.lasthopesoftware.bluewater.client.connection.requests

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.namehillsoftware.handoff.promises.Promise
import java.io.IOException
import java.net.URL

class FakeHttpConnection : HttpPromiseClient {
	private val requests = ArrayList<URL>()

	private val mappedResponses = HashMap<URL, (URL) -> PassThroughHttpResponse>()

	val recordedRequests: List<URL>
		get() = requests

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
		mappedResponses[url] = response
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

	private fun getResponse(url: URL): HttpResponse {
		val mappedResponse = mappedResponses[url]
//		if (mappedResponse == null) {
//			val optionalResponse = mappedResponses.keys
//				.find { set -> set.all { sp -> params.any { p -> p.matches(Regex(sp)) } } }
//			if (optionalResponse != null) mappedResponse = mappedResponses[optionalResponse]
//		}
		if (mappedResponse == null) return PassThroughHttpResponse(
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
}
