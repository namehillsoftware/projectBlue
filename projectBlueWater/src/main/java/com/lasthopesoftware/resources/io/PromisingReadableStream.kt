package com.lasthopesoftware.resources.io

import com.lasthopesoftware.promises.PromiseMachines
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise
import io.ktor.utils.io.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

interface PromisingReadableStream : PromisingCloseable {
	companion object {
		const val DefaultBufferSize = 8192
		const val MaxBufferSize = Int.MAX_VALUE - 8
	}

	fun promiseRead(): Promise<Int>
	fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int>
	fun promiseReadAllBytes(): Promise<ByteArray> {
		val len = Int.MAX_VALUE
		val bufsRef = AtomicReference<MutableList<ByteArray>?>(null)
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
								if (cs.isCancelled) throw CancellationException()
								val n = n ?: 0
								val remaining = remainingRef.addAndGet(-n)
								val localNRead = nread.addAndGet(n)
								val buf = bufRef.get()
								if (cs.isCancelled) throw CancellationException()
								promiseRead(buf, localNRead, (buf.size - localNRead).coerceAtMost(remaining))
							}
						}
						.then {
							val localNread = nread.get()
							if (localNread > 0) {
								if (totalRef.addAndGet(localNread) > MaxBufferSize) {
									throw OutOfMemoryError("Required array size too large")
								}

								if (cs.isCancelled) throw CancellationException()
								val buf = bufRef.updateAndGet { b ->
									if (localNread >= b.size) b
									else b.copyOfRange(0, localNread)
								}

								val oldBuf = resultRef.getAndSet( buf)
								if (oldBuf != null) {
									bufsRef.updateAndGet { b -> b ?: ArrayList() }?.add(oldBuf)
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
					cs.isCancelled -> throw CancellationException()
					bufs != null -> {
						resultRef.set(null)
						val result = ByteArray(total)
						var offset = 0
						remainingRef.set(total)
						for (b in bufs) {
							if (cs.isCancelled) throw CancellationException()

							val count = min(b.size, remainingRef.get())
							System.arraycopy(b, 0, result, offset, count)
							offset += count
							remainingRef.addAndGet(-count)
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

