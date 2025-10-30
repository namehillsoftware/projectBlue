package com.lasthopesoftware.resources.io

import com.lasthopesoftware.bluewater.shared.drainQueue
import com.lasthopesoftware.promises.PromiseMachines
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.Volatile

class PromisingChannel(pipeSize: Int = DEFAULT_PIPE_SIZE) : PromisingReadableStream {

	private val eaters = ConcurrentLinkedQueue<AbstractEater>()
	private val currentTaster = AtomicReference(Taster())
	private val sync = Any()

	@Volatile
	private var closedByWriter = false

	@Volatile
	private var closedByReader = false

	/**
	 * The circular buffer into which incoming data is placed.
	 * @since   1.1
	 */
	private val buffer = ByteArray(pipeSize)

	/**
	 * The index of the position in the circular buffer at which the
	 * next byte of data will be stored when received from the connected
	 * piped output stream. `in<0` implies the buffer is empty,
	 * `in==out` implies the buffer is full
	 * @since   1.1
	 */
	private var `in` = -1

	/**
	 * The index of the position in the circular buffer at which the next
	 * byte of data will be read by this piped input stream.
	 * @since   1.1
	 */
	private var out = 0

	val writableStream: PromisingWritableStream<*> by lazy { ConnectedWritableStream() }

	/**
	 * Reads the next byte of data from this piped input stream. The
	 * value byte is returned as an `int` in the range
	 * `0` to `255`.
	 * This method blocks until input data is available, the end of the
	 * stream is detected, or an exception is thrown.
	 *
	 * @return     the next byte of data, or `-1` if the end of the
	 * stream is reached.
	 * @exception  java.io.IOException  if the pipe is
	 * [unconnected][.connect],
	 * [ `broken`](#BROKEN), closed,
	 * or if an I/O error occurs.
	 */
	override fun promiseRead(): Promise<Int> = synchronized(sync) {
		when {
			closedByReader -> throw IOException("Pipe closed")
			closedByWriter && tryEating() -> (-1).toPromise()
			else -> {
				val snacker = Snacker()
				eaters.add(snacker)
				tryEating()
				snacker
			}
		}
	}

	/**
	 * Reads up to `len` bytes of data from this piped input
	 * stream into an array of bytes. Less than `len` bytes
	 * will be read if the end of the data stream is reached or if
	 * `len` exceeds the pipe's buffer size.
	 * If `len ` is zero, then no bytes are read and 0 is returned;
	 * otherwise, the method blocks until at least 1 byte of input is
	 * available, end of the stream has been detected, or an exception is
	 * thrown.
	 *
	 * @param      b     the buffer into which the data is read.
	 * @param      off   the start offset in the destination array `b`
	 * @param      len   the maximum number of bytes read.
	 * @return     the total number of bytes read into the buffer, or
	 * `-1` if there is no more data because the end of
	 * the stream has been reached.
	 * @exception  NullPointerException If `b` is `null`.
	 * @exception  IndexOutOfBoundsException If `off` is negative,
	 * `len` is negative, or `len` is greater than
	 * `b.length - off`
	 * @exception  IOException if the pipe is [ `broken`](#BROKEN),
	 * [unconnected][.connect],
	 * closed, or if an I/O error occurs.
	 */
	override fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int> = synchronized(sync) {
		when {
			closedByReader -> throw IOException("Pipe closed")
			off < 0 || len < 0 || len > b.size - off -> throw IndexOutOfBoundsException()
			len == 0 -> 0.toPromise()
			closedByWriter && tryEating() -> (-1).toPromise()
			else -> {
				val feeder = Eater(b, off, len)
				eaters.add(feeder)
				if (`in` < 0) `in` = 0
				tryEating()
				feeder
			}
		}
	}

	/**
	 * Returns the number of bytes that can be read from this input
	 * stream without blocking.
	 *
	 * @return the number of bytes that can be read from this input stream
	 * without blocking, or `0` if this input stream has been
	 * closed by invoking its [.close] method, or if the pipe
	 * is [unconnected][.connect], or
	 * [ `broken`](#BROKEN).
	 *
	 * @exception  IOException  if an I/O error occurs.
	 * @since   1.0.2
	 */
	override fun available(): Int {
		return when {
			`in` < 0 -> 0
			`in` == out -> buffer.size
			`in` > out -> `in` - out
			else -> `in` + buffer.size - out
		}
	}

	override fun promiseClose(): Promise<Unit> {
		closedByReader = !closedByWriter
		synchronized(sync) {
			val promisedClosed = Promise.whenAll(
				eaters.drainQueue().map { it.apply { cancel() } }
			)
			`in` = -1
			return promisedClosed.unitResponse()
		}
	}

	private fun tryEating(): Boolean = synchronized(sync) {
		var taster: AbstractEater? = null
		while (`in` >= 0) {
			val feeder = eaters.peek()
			if (feeder == null) {
				break
			}

			val maxAmount =
				if (`in` > out) (buffer.size - out).coerceAtMost(`in` - out)
				else buffer.size - out

			val fed = feeder.eat(buffer, out, maxAmount)
			out += fed
			if (fed > 0) {
				taster?.apply {
					eat(buffer, out, maxAmount)
					taster = null
				}
			}

			when {
				feeder.isFed -> eaters.poll()
				feeder == currentTaster.get() -> {
					taster = eaters.poll()
					if (eaters.peek() != null) continue
				}
			}

			if (out >= buffer.size) out = 0
			if (`in` == out) `in` = -1
		}

		taster?.takeUnless { it.isFed }?.also(eaters::offer)

		return eaters.isEmpty()
	}

	/**
	 * Receives data into an array of bytes.  This method will
	 * block until some input is available.
	 * @param b the buffer into which the data is received
	 * @param off the start offset of the data
	 * @param len the maximum number of bytes received
	 * @exception IOException If the pipe is [ broken](#BROKEN),
	 * [unconnected][.connect],
	 * closed,or if an I/O error occurs.
	 */
	private fun promiseReceive(b: ByteArray, off: Int, len: Int): Promise<Unit> {
		val offRef = AtomicInteger(off)
		checkStateForReceive()
		val bytesToTransferRef = AtomicInteger(len)
		return PromiseMachines.loop { _, cancellable ->
			synchronized(sync) {
				when {
					bytesToTransferRef.get() <= 0 -> {
						cancellable.cancel()
						tryEating()
						Unit.toPromise()
					}
					// Channel is full, wait for space to be released
					`in` == out -> awaitSpace()
					else -> {
						val nextTransferAmount = when {
							out < `in` -> buffer.size - `in`
							`in` == -1 -> {
								out = 0
								`in` = 0
								buffer.size
							}
							else -> out - `in`
						}.coerceAtMost(bytesToTransferRef.get())

						assert(nextTransferAmount > 0)
						val currentOffset = offRef.get()
						b.copyInto(buffer, `in`, currentOffset, currentOffset + nextTransferAmount)
						bytesToTransferRef.addAndGet(-nextTransferAmount)
						offRef.addAndGet(nextTransferAmount)
						`in` += nextTransferAmount
						if (`in` >= buffer.size) {
							`in` = 0
						}
						Unit.toPromise()
					}
				}
			}
		}
	}

	@Throws(IOException::class)
	private fun checkStateForReceive() {
		if (closedByWriter || closedByReader) {
			throw IOException("Pipe closed")
		}
	}

	private fun awaitSpace(): Promise<Unit> {
		return PromiseMachines.loop { _, cancellable ->
			synchronized(sync) {
				when {
					// Space available
					`in` != out -> {
						cancellable.cancel()
						Unit.toPromise()
					}
					!tryEating() -> Unit.toPromise()
					else -> {
						val taster = currentTaster.updateAndGet {
							if (it.isFed) Taster()
							else it
						}
						eaters.offer(taster)
						tryEating()
						// Wait to get a taste, once you have a result check again for space.
						taster.unitResponse()
					}
				}
			}
		}
	}

	/**
	 * Notifies all waiting threads that the last byte of data has been
	 * received.
	 */
	private fun receivedLast(): Promise<Unit> {
		tryEating()
		closedByWriter = !closedByReader
		return promiseClose()
	}

	private inner class ConnectedWritableStream() : PromisingWritableStream<ConnectedWritableStream>,
        ImmediateResponse<Unit, ConnectedWritableStream> {
		override fun promiseWrite(buffer: ByteArray, offset: Int, length: Int): Promise<ConnectedWritableStream> =
			promiseReceive(buffer, offset, length).then(this)

		override fun flush(): Promise<ConnectedWritableStream> = awaitSpace().then(this)

		override fun promiseClose(): Promise<Unit> = receivedLast()

		override fun respond(resolution: Unit?): ConnectedWritableStream = this
	}


	private abstract class AbstractEater() : Promise<Int>(), CancellationResponse {
		init {
			awaitCancellation(this)
		}

		override fun cancellationRequested() {
			resolve(0)
		}

		abstract val isFed: Boolean
		abstract val capacity: Int
		abstract fun eat(byteArray: ByteArray, offset: Int, maxAmount: Int): Int
	}

	private class Eater(
		private val destination: ByteArray,
		private val destinationOffset: Int,
		override val capacity: Int,
	) : AbstractEater() {

		private val sync = Any()

		@Volatile
		private var read = 0

		@Volatile
		private var isCancelled = false

		override val isFed
			get() = read >= capacity

		override fun cancellationRequested() {
			isCancelled = true
			synchronized(sync) {
				resolve(read)
			}
		}

		override fun eat(byteArray: ByteArray, offset: Int, maxAmount: Int): Int {
			return try {
				synchronized(sync) {
					if (isCancelled) return 0
					val amountToRead = (capacity - read).coerceAtMost(byteArray.size - offset).coerceAtMost(maxAmount)
					val endIndex = offset + amountToRead
					if (isCancelled) return 0
					byteArray.copyInto(destination, destinationOffset + read, startIndex = offset, endIndex = endIndex)
					read += amountToRead
					if (isFed) {
						resolve(read)
					}
					amountToRead
				}
			} catch (e: Throwable) {
				reject(e)
				0
			}
		}
	}

	private class Snacker() : AbstractEater() {
		private val sync = Any()

		@Volatile
		override var isFed = false
			private set
		override val capacity: Int = 1

		override fun eat(byteArray: ByteArray, offset: Int, maxAmount: Int) = synchronized(sync) {
			try {
				val result = when {
					byteArray.isEmpty() -> -1
					offset >= byteArray.size -> 0
					else -> byteArray[0].toInt()
				}
				resolve(result)
				isFed = true
				result.coerceAtLeast(0)
			} catch (e: Throwable) {
				reject(e)
				0
			}
		}
	}

	private inner class Taster() : AbstractEater() {
		private val sync = Any()

		@Volatile
		override var isFed = false
			private set
		override val capacity = 0

		override fun eat(byteArray: ByteArray, offset: Int, maxAmount: Int): Int = synchronized(sync) {
			if (`in` == out) return 0

			resolve(0)
			isFed = true
			return 0
		}
	}

	companion object {
		private const val DEFAULT_PIPE_SIZE = 1024
	}
}
