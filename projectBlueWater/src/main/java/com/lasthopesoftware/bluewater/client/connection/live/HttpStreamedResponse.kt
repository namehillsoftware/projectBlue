package com.lasthopesoftware.bluewater.client.connection.live

import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.io.PromisingReadableStream
import com.lasthopesoftware.resources.io.PromisingReadableStreamWrapper
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateCancellableResponse
import io.ktor.utils.io.CancellationException

class HttpStreamedResponse : ImmediateCancellableResponse<HttpResponse, PromisingReadableStream>,
    PromisingReadableStream, (HttpResponse, CancellationSignal) -> PromisingReadableStream {
	private lateinit var byteStream: PromisingReadableStream

	override fun invoke(response: HttpResponse, cancellationSignal: CancellationSignal): PromisingReadableStream =
		respond(response, cancellationSignal)

	override fun respond(response: HttpResponse, cancellationSignal: CancellationSignal): PromisingReadableStream {
		if (cancellationSignal.isCancelled) throw CancellationException("Streamed response cancelled.")

		byteStream =
			if (response.code != 404) response.body
			else PromisingReadableStreamWrapper(emptyByteArray.inputStream(), null)

		return this
	}

	override fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int> = byteStream.promiseRead(b, off, len)

	override fun available(): Int = byteStream.available()

	override fun toString(): String = byteStream.toString()
	override fun promiseClose(): Promise<Unit> = byteStream.promiseClose()
}
