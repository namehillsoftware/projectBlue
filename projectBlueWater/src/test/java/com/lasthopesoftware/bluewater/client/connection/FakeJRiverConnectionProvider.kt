package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.namehillsoftware.handoff.promises.Promise
import java.io.IOException
import java.net.URL

abstract class FakeJRiverConnectionProvider : ProvideConnections {
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

	override fun promiseResponse(path: String, vararg params: String): Promise<HttpResponse> {
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

	private fun getResponse(vararg params: String): HttpResponse {
		var mappedResponse = mappedResponses[setOf(*params)]
		if (mappedResponse == null) {
			val optionalResponse = mappedResponses.keys
				.find { set -> set.all { sp -> params.any { p -> p.matches(Regex(sp)) } } }
			if (optionalResponse != null) mappedResponse = mappedResponses[optionalResponse]
		}
		if (mappedResponse == null) return PassThroughHttpResponse(
			code = 404,
			message = "Not Found",
			"Not found".toByteArray().inputStream()
		)
		try {
			val result = mappedResponse(params)
			return PassThroughHttpResponse(
				code = result.code,
				message = "Ok",
				body = result.response.inputStream()
			)
		} catch (io: IOException) {
			throw io
		} catch (error: Throwable) {
			throw RuntimeException(error)
		}
	}

	override val serverConnection: ServerConnection
		get() = ServerConnection("auth", "test", 80)

	override fun <T> getConnectionKey(key: T): UrlKeyHolder<T> = UrlKeyHolder(serverConnection.baseUrl, key)

	override fun getFileUrl(serviceFile: ServiceFile): URL {
		TODO("Not yet implemented")
	}

	override fun promiseIsConnectionPossible(): Promise<Boolean> = true.toPromise()
}
