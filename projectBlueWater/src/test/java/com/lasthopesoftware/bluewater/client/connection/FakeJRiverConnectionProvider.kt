package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.url.JRiverUrlBuilder
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http.RealResponseBody
import okio.Buffer
import java.io.IOException
import java.net.URL

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

	fun mapResponse(response: (Array<out String>) -> FakeConnectionResponseTuple, path: String, vararg params: String) {
		val paramsSet = setOf(path, *params)
		mappedResponses[paramsSet] = response
	}

	override fun promiseResponse(path: String, vararg params: String): Promise<Response> {
		val requestParams = arrayOf(path, *params)
		requests.add(requestParams)

		return try {
			Promise(getResponse(*requestParams))
		} catch (e: IOException) {
			Promise(e)
		} catch (e: RuntimeException) {
			Promise(e.cause)
		}
	}

	private fun getResponse(vararg params: String): Response {
		val builder = Request.Builder()
		builder.url(JRiverUrlBuilder.getUrl(serverConnection.baseUrl, params.first(), *params.drop(1).toTypedArray()))
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

	override val serverConnection: ServerConnection
		get() = ServerConnection("auth", "test", 80)

	override fun <T> getConnectionKey(key: T): UrlKeyHolder<T> = UrlKeyHolder(serverConnection.baseUrl, key)

	override fun getDataAccess(): RemoteLibraryAccess = JRiverLibraryAccess(this)

	override fun getFileUrl(serviceFile: ServiceFile): URL {
		TODO("Not yet implemented")
	}

	override fun promiseIsConnectionPossible(): Promise<Boolean> = true.toPromise()
}
