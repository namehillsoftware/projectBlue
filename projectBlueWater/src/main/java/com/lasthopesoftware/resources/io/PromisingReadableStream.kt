package com.lasthopesoftware.resources.io

import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.shared.drainQueue
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.PromiseMachines
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise
import io.ktor.utils.io.CancellationException
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

interface PromisingReadableStream : PromisingCloseable {
	companion object {
		const val DefaultBufferSize = 8192
		const val MaxBufferSize = Int.MAX_VALUE - 8
		private val logger by lazyLogger<PromisingReadableStream>()

		fun PromisingReadableStream.readingCancelledException() =
			CancellationException("Reading stream was cancelled with ${available()} bytes remaining.")
	}

	fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int>
	fun promiseReadAllBytes(): Promise<ByteArray> {
		val len = Int.MAX_VALUE
		val bufsRef = AtomicReference<LinkedList<ByteArray>?>(null)
		val resultRef = AtomicReference<ByteArray?>()
		val totalRef = AtomicInteger()
		val remainingRef = AtomicInteger(len)
		return Promise.Proxy { cs ->
			PromiseMachines.loop<Int> { outer, cancellable ->
				if (remainingRef.get() <= 0 || (outer != null && outer < 0)) {
					cancellable.cancel()
					return@loop 0.toPromise()
				} else {
					if (cs.isCancelled) throw CancellationException()

					val bufRef = AtomicReference(ByteArray(min(remainingRef.get(), DefaultBufferSize)))
					val nread = AtomicInteger()

					// read to EOF which may read more or less than buffer size

					PromiseMachines
						.loop<Int> { n, cancellable ->
							if (n != null && n <= 0) {
								cancellable.cancel()
								n.toPromise()
							} else {
								if (cs.isCancelled) throw readingCancelledException()
								val n = n ?: 0
								val remaining = remainingRef.addAndGet(-n)
								val localNRead = nread.addAndGet(n)
								val buf = bufRef.get()
								if (cs.isCancelled) throw readingCancelledException()

								val readLength = (buf.size - localNRead).coerceAtMost(remaining)
								if (readLength <= 0) {
									if (BuildConfig.DEBUG) {
										logger.debug("Buffer full, continuing.")
									}
									cancellable.cancel()
									0.toPromise()
								} else {
									if (BuildConfig.DEBUG) {
										logger.debug("Reading {} bytes from stream...", readLength)
									}
									promiseRead(buf, localNRead, readLength).also(cs::doCancel)
								}
							}
						}
						.then {
							val localNread = nread.get()
							if (BuildConfig.DEBUG) {
								logger.debug("Read {} bytes from stream.", localNread)
							}
							if (localNread > 0) {
								if (totalRef.addAndGet(localNread) > MaxBufferSize) {
									throw OutOfMemoryError("Required array size too large")
								}

								if (cs.isCancelled) throw readingCancelledException()
								val buf = bufRef.updateAndGet { b ->
									if (localNread >= b.size) b
									else b.copyOfRange(0, localNread)
								}

								val oldBuf = resultRef.getAndSet( buf)
								if (oldBuf != null) {
									bufsRef.updateAndGet { b -> b ?: LinkedList<ByteArray>().apply { add(oldBuf) } }?.add(buf)
								}
							}

							it
						}
				}
			}.then {
				val result = resultRef.get()
				val total = totalRef.get()
				val bufs = bufsRef.get()
				when {
					cs.isCancelled -> throw readingCancelledException()
					bufs != null -> {
						resultRef.set(null)
						val result = ByteArray(total)
						var offset = 0
						var remaining = total
						for (b in bufs.drainQueue()) {
							if (cs.isCancelled) throw readingCancelledException()

							val count = min(b.size, remainingRef.get())
							System.arraycopy(b, 0, result, offset, count)
							offset += count
							remaining -= count
						}

						result
					}

					result == null -> emptyByteArray
					result.size == total -> result
					else -> result.copyOf(total)
				}
			}
		}
	}

	fun available(): Int
}

