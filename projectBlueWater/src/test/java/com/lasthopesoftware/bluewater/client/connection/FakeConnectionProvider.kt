package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http.RealResponseBody
import okio.Buffer
import java.io.IOException
import java.util.*

open class FakeConnectionProvider : IConnectionProvider {
	private val mappedResponses = HashMap<Set<String>, (Array<out String>) -> FakeConnectionResponseTuple>()
	fun mapResponse(response: (Array<out String>) -> FakeConnectionResponseTuple, vararg params: String) {
		val paramsSet = setOf(*params)
		mappedResponses[paramsSet] = response
	}

	override fun promiseResponse(vararg params: String): Promise<Response> {
		return try {
			Promise(getResponse(*params))
		} catch (e: IOException) {
			Promise(e)
		} catch (e: RuntimeException) {
			Promise(e.cause)
		}
	}

	@Throws(IOException::class)
	private fun getResponse(vararg params: String): Response {
		val builder = Request.Builder()
		builder.url(urlProvider.getUrl(*params)!!)
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

	override val urlProvider: IUrlProvider
		get() = MediaServerUrlProvider(null, "test", 80)

	override fun promiseSentPacket(packets: ByteArray): Promise<Unit> {
		return Promise(Unit)
	}
}
