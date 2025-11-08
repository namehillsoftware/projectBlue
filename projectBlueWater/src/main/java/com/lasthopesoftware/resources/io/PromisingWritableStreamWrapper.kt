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

/**
 * An implementation of a [PromisingWritableStream] that is backed by an [OutputStream].
 *
 * This class adapts a standard OutputStream into a promise-based asynchronous stream.
 * All read and close operations are performed asynchronously on a specified executor.
 *
 * @param outputStream The underlying [OutputStream] to read data from.
 * @param transferExecutor The [Executor] on which the asynchronous I/O operations (read, close) will be performed.
 *                         Defaults to [ThreadPools.io]. If null, will execute on calling thread.
 */
class PromisingWritableStreamWrapper(
	private val outputStream: OutputStream,
	private val transferExecutor: Executor? = ThreadPools.io,
) : PromisingWritableStream {
	override fun promiseWrite(buffer: ByteArray, offset: Int, length: Int) = transferExecutor?.preparePromise {
		outputStream.write(buffer, offset, length)
		length
	} ?: outputStream.write(buffer, offset, length).run { length }.toPromise()

	override fun promiseFlush(): Promise<Unit> = transferExecutor?.preparePromise {
		outputStream.flush()
	} ?: outputStream.flush().toPromise()

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
