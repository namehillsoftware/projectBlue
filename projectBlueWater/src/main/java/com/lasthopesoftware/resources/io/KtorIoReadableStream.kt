package com.lasthopesoftware.resources.io

import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

class KtorIoReadableStream(
	private val channel: ByteReadChannel,
	private val scope: CoroutineScope,
): Promise<Unit>(), PromisingReadableStream, CancellationResponse {
	init {
	    awaitCancellation(this)
	}

	override fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int> = scope.async {
		channel.readAvailable(b, off, len)
	}.toPromise()

	override fun available(): Int = channel.availableForRead

	override fun promiseClose(): Promise<Unit> {
		channel.cancel()
		val closedCause = channel.closedCause
		if (closedCause == null) resolve(Unit)
		else reject(closedCause)
		return Unit.toPromise()
	}

	override fun cancellationRequested() {
		promiseClose()
	}
}
