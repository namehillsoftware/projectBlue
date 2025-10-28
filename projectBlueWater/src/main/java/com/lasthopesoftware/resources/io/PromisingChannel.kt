package com.lasthopesoftware.resources.io

import com.lasthopesoftware.bluewater.shared.drainQueue
import com.lasthopesoftware.promises.PromiseMachines
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.Volatile

class PromisingChannel(pipeSize: Int = DEFAULT_PIPE_SIZE) : PromisingReadableStream {

	private val feeders = ConcurrentLinkedQueue<AbstractFeeder>()
	private val sync = Any()

	@Volatile
	private var closedByWriter: Boolean = false

	@Volatile
	private var closedByReader: Boolean = false

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
	override fun promiseRead(): Promise<Int> {
		if (closedByReader) {
			throw IOException("Pipe closed")
		}

		val sipper = Sipper()
		feeders.add(sipper)
		tryFeeding()
		return sipper
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
	override fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int> {
		if (off < 0 || len < 0 || len > b.size - off) {
			throw IndexOutOfBoundsException()
		} else if (len == 0) {
			return 0.toPromise()
		}

		val feeder = Feeder(b, off, len)
		feeders.add(feeder)
		tryFeeding()
		return feeder
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
				feeders.drainQueue().map { it.apply { cancel() } }
			)
			`in` = -1
			return promisedClosed.unitResponse()
		}
	}

	private fun tryFeeding() {
		synchronized(sync) {
			var fed = 0
			while (`in` >= 0) {
				val feeder = feeders.peek()
				if (feeder == null) {
					break
				}

				val maxAmount =
					if (`in` > out) (buffer.size - out).coerceAtMost(`in` - out)
					else buffer.size - out

				fed += feeder.feed(buffer, out, maxAmount)
				if (fed >= feeder.capacity) {
					fed = 0
					feeders.poll()
				}
				out += fed
				if (out >= buffer.size) out = 0
				if (`in` == out) `in` = -1
			}
		}
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
		var off = off
		checkStateForReceive()
		var bytesToTransfer = len
		return PromiseMachines.loop { _, cancellable ->
			synchronized(sync) {
				when {
					bytesToTransfer <= 0 -> {
						cancellable.cancel()
						tryFeeding().toPromise()
					}

					`in` == out -> awaitSpace()
					else -> synchronized(sync) {
						val nextTransferAmount = when {
							out < `in` -> buffer.size - `in`
							`in` == -1 -> {
								out = 0
								`in` = 0
								buffer.size
							}

							else -> out - `in`
						}.coerceAtMost(bytesToTransfer)

						assert(nextTransferAmount > 0)
						b.copyInto(buffer, `in`, off, off + nextTransferAmount)
						bytesToTransfer -= nextTransferAmount
						off += nextTransferAmount
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
				if (`in` != out) {
					cancellable.cancel()
					Unit.toPromise()
				} else {
					val taster = Taster()
					feeders.offer(taster)
					tryFeeding()
					taster.unitResponse()
				}
			}
		}
	}

	/**
	 * Notifies all waiting threads that the last byte of data has been
	 * received.
	 */
	private fun receivedLast(): Promise<Unit> {
		tryFeeding()
		closedByWriter = true
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


	private abstract class AbstractFeeder() : Promise<Int>() {
		init {
			awaitCancellation { resolve(0) }
		}

		abstract val capacity: Int
		abstract fun feed(byteArray: ByteArray, offset: Int, maxAmount: Int): Int
	}

	private class Feeder(
		private val destination: ByteArray,
		private val destinationOffset: Int,
		override val capacity: Int,
	) : AbstractFeeder() {

		@Volatile
		private var read = 0

		@Volatile
		private var isCancelled = false

		init {
			awaitCancellation {
				isCancelled = true
				resolve(read)
			}
		}

		override fun feed(byteArray: ByteArray, offset: Int, maxAmount: Int): Int {
			return try {
				if (isCancelled) return read
				val amountToRead = (capacity - read).coerceAtMost(byteArray.size - offset).coerceAtMost(maxAmount)
				val endIndex = offset + amountToRead
				if (isCancelled) return read
				byteArray.copyInto(destination, destinationOffset + read, startIndex = offset, endIndex = endIndex)
				read += amountToRead
				if (read >= capacity) {
					resolve(read)
				}
				amountToRead
			} catch (e: Throwable) {
				reject(e)
				0
			}
		}
	}

	private class Sipper() : AbstractFeeder() {
		override val capacity: Int = 1

		override fun feed(byteArray: ByteArray, offset: Int, maxAmount: Int) =
			try {
				resolve(if (byteArray.isEmpty()) -1 else byteArray[0].toInt())
				1
			} catch (e: Throwable) {
				reject(e)
				0
			}
	}

	private class Taster() : AbstractFeeder() {
		override val capacity: Int = 0
		override fun feed(byteArray: ByteArray, offset: Int, maxAmount: Int): Int {
			resolve(0)
			return 0
		}
	}

	companion object {
		private const val DEFAULT_PIPE_SIZE = 1024
	}
}
