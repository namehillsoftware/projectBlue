package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider
import com.lasthopesoftware.bluewater.client.connection.url.ProvideUrls
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http.RealResponseBody
import okio.Buffer
import java.io.IOException

open class FakeJRiverConnectionProvider : ProvideConnections {
	private val requests = ArrayList<Array<out String>>()

	private val mappedResponses = HashMap<Set<String>, (Array<out String>) -> FakeConnectionResponseTuple>()

	val recordedRequests: List<Array<out String>>
		get() = requests

	init {
		mapResponse(
			{ FakeConnectionResponseTuple(200, ByteArray(0)) },
			"File/GetFile",
			"File=.*",
			"Quality=medium",
			"Conversion=Android",
			"Playback=0"
		)
	}

	fun setupFile(serviceFile: ServiceFile, fileProperties: Map<String, String>) {
		mapResponse(
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
				FakeConnectionResponseTuple(200, returnXml.toString().toByteArray())
			},
			"File/GetInfo",
			"File=" + serviceFile.key
		)
	}

	fun mapResponse(response: (Array<out String>) -> FakeConnectionResponseTuple, vararg params: String) {
		val paramsSet = setOf(*params)
		mappedResponses[paramsSet] = response
	}

	override fun promiseResponse(vararg params: String): Promise<Response> {
		requests.add(params)

		return try {
			Promise(getResponse(*params))
		} catch (e: IOException) {
			Promise(e)
		} catch (e: RuntimeException) {
			Promise(e.cause)
		}
	}

	private fun getResponse(vararg params: String): Response {
		val builder = Request.Builder()
		builder.url(urlProvider.getUrl(*params))
		val buffer = Buffer()
		val responseBuilder = Response.Builder()
		responseBuilder
			.request(builder.build())
			.protocol(Protocol.HTTP_1_1)
			.message("Not Found")
			.body(RealResponseBody(null, 0, buffer.write("Not found".toByteArray())))
			.code(404)
		var mappedResponse = mappedResponses[setOf(*params)]
		if (mappedResponse == null) {
			val optionalResponse = mappedResponses.keys
				.find { set -> set.all { sp -> params.any { p -> p.matches(Regex(sp)) } } }
			if (optionalResponse != null) mappedResponse = mappedResponses[optionalResponse]
		}
		if (mappedResponse == null) return responseBuilder.build()
		try {
			buffer.clear()
			val result = mappedResponse(params)
			buffer.write(result.response)
			responseBuilder.code(result.code)
			responseBuilder.body(RealResponseBody(null, result.response.size.toLong(), buffer))
		} catch (io: IOException) {
			throw io
		} catch (error: Throwable) {
			throw RuntimeException(error)
		}
		return responseBuilder.build()
	}

	override val urlProvider: ProvideUrls
		get() = MediaServerUrlProvider("auth", "test", 80)

	override fun <T> getConnectionKey(key: T): UrlKeyHolder<T> = UrlKeyHolder(urlProvider.baseUrl, key)

	override fun getDataAccess(): RemoteLibraryAccess = JRiverLibraryAccess(this)

	override fun promiseIsConnectionPossible(): Promise<Boolean> = true.toPromise()
}
