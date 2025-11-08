package com.lasthopesoftware.bluewater.client.connection.live

import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.io.PromisingReadableStream
import com.lasthopesoftware.resources.io.PromisingReadableStreamWrapper
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.io.ByteArrayInputStream

class HttpStreamedResponse : ImmediateResponse<HttpResponse?, PromisingReadableStream>,
    PromisingReadableStream {
	private var savedResponse: HttpResponse? = null
	private lateinit var byteStream: PromisingReadableStream

	override fun respond(response: HttpResponse?): PromisingReadableStream {
		savedResponse = response

		byteStream = response
			?.takeIf { it.code != 404 }
			?.run { body }
			?: PromisingReadableStreamWrapper(ByteArrayInputStream(emptyByteArray))

		return this
	}

	override fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int> = byteStream.promiseRead(b, off, len)

	override fun available(): Int = byteStream.available()

	override fun toString(): String = byteStream.toString()
	override fun promiseClose(): Promise<Unit> = byteStream.promiseClose()
}
