package com.lasthopesoftware.resources.io

import com.lasthopesoftware.promises.PromiseMachines
import com.lasthopesoftware.promises.extensions.guaranteedUnitResponse
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import java.io.OutputStream
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

class PromisingWritableStreamWrapper(
	private val outputStream: OutputStream,
	private val transferExecutor: Executor = ThreadPools.io,
) : PromisingWritableStream {
	override fun promiseWrite(buffer: ByteArray, offset: Int, length: Int) = transferExecutor.preparePromise {
		outputStream.write(buffer, offset, length)
		length
	}

	override fun promiseFlush(): Promise<Unit> = transferExecutor.preparePromise {
		outputStream.flush()
	}

	override fun promiseClose(): Promise<Unit> = promiseFlush().must(outputStream::close).guaranteedUnitResponse()

	override fun promiseCopyFrom(inputStream: PromisingReadableStream, bufferSize: Int): Promise<Int> {
		val buffer = ByteArray(bufferSize)
		val isFinished = AtomicBoolean()
		return Promise.Proxy { cs ->
			PromiseMachines.loop { totalBytes, cancellable ->
				val currentTotal = totalBytes ?: 0
				if (isFinished.get()) currentTotal.toPromise()
				else inputStream
					.promiseRead(buffer, 0, bufferSize)
					.also(cs::doCancel)
					.then { bytes, ct ->
						if (bytes <= 0) {
							isFinished.set(true)
							cancellable.cancel()
							return@then currentTotal
						}

						if (ct.isCancelled) throw CancellationException("promiseCopyFrom was cancelled.")
						outputStream.write(buffer, 0, bytes)

						if (ct.isCancelled) throw CancellationException("promiseCopyFrom was cancelled.")
						currentTotal + bytes
					}
			}
		}
	}
}
