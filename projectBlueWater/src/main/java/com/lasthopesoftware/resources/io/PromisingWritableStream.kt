package com.lasthopesoftware.resources.io

import com.lasthopesoftware.promises.PromiseMachines
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

interface PromisingWritableStream : PromisingCloseable {
	fun promiseWrite(buffer: ByteArray, offset: Int, length: Int): Promise<Int>
	fun promiseFlush(): Promise<Unit>

	override fun promiseClose() = promiseFlush()

	fun promiseCopyFrom(inputStream: PromisingReadableStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Promise<Int> {
		val buffer = ByteArray(bufferSize)
		val isFinished = AtomicBoolean()
		return Promise.Proxy { cs ->
			PromiseMachines.loop { totalBytes, cancellable ->
				val currentTotal = totalBytes ?: 0
				if (isFinished.get()) currentTotal.toPromise()
				else inputStream
					.promiseRead(buffer, 0, bufferSize)
					.also(cs::doCancel)
					.eventually { bytes ->
						if (bytes <= 0) {
							isFinished.set(true)
							cancellable.cancel()
							currentTotal.toPromise()
						} else {
							promiseWrite(buffer, 0, bytes)
								.also(cs::doCancel)
								.then { bytes, ct ->
									if (ct.isCancelled) throw CancellationException("promiseCopyFrom was cancelled.")
									currentTotal + bytes
								}
								.also(cs::doCancel)
						}
					}
			}
		}
	}
}

interface RejectablePromisingWritableStream : PromisingWritableStream, RejectableCloseable
