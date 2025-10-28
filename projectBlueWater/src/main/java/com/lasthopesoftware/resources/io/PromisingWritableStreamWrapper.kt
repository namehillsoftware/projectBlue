package com.lasthopesoftware.resources.io

import com.lasthopesoftware.promises.PromiseMachines
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.guaranteedUnitResponse
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

class PromisingWritableStreamWrapper(private val outputStream: OutputStream) : PromisingWritableStream<PromisingWritableStreamWrapper> {
	override fun promiseWrite(buffer: ByteArray, offset: Int, length: Int) = ThreadPools.io.preparePromise {
		outputStream.write(buffer, offset, length)
		this
	}

	override fun flush(): Promise<PromisingWritableStreamWrapper> = ThreadPools.io.preparePromise {
		outputStream.flush()
		this
	}

	override fun promiseClose(): Promise<Unit> = flush().must(outputStream::close).guaranteedUnitResponse()

	fun promiseCopyFrom(inputStream: PromisingReadableStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Promise<Int> {
		val buffer = ByteArray(bufferSize)
		val isFinished = AtomicBoolean()
		return PromiseMachines.loop { totalBytes, cancellable ->
			val currentTotal = totalBytes ?: 0
			if (isFinished.get()) currentTotal.toPromise()
			else inputStream
				.promiseRead(buffer, 0, bufferSize)
				.cancelBackThen { bytes, ct ->
					if (bytes <= 0) {
						isFinished.set(true)
						cancellable.cancel()
						return@cancelBackThen currentTotal
					}

					if (ct.isCancelled) throw CancellationException("promiseCopyFrom was cancelled.")
					outputStream.write(buffer, 0, bytes)

					if (ct.isCancelled) throw CancellationException("promiseCopyFrom was cancelled.")
					currentTotal + bytes
				}
		}
	}
}
