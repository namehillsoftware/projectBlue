package com.lasthopesoftware.resources.io

import com.lasthopesoftware.bluewater.shared.drainQueue
import com.lasthopesoftware.promises.PromiseMachines
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.namehillsoftware.handoff.promises.Promise
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.Volatile

interface PromisingInputStream<T : PromisingInputStream<T>> : Closeable, PromisingCloseable {
	fun promiseRead(): Promise<Int>
	fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int>
	fun available(): Int
}

class PipedPromisingInputStream : PromisingInputStream<PipedPromisingInputStream> {
	private val feeders = ConcurrentLinkedQueue<AbstractFeeder>()
	private val sync = Any()

	@Volatile
	var closedByWriter: Boolean = false

	@Volatile
	var closedByReader: Boolean = false

	var connected: Boolean = false

	/**
	 * The circular buffer into which incoming data is placed.
	 * @since   1.1
	 */
	private val buffer = ByteArray(DEFAULT_PIPE_SIZE)

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

//	@JvmOverloads
//	constructor(src: PipedOutputStream, pipeSize: Int = DEFAULT_PIPE_SIZE) {
//		initPipe(pipeSize)
//		connect(src)
//	}
//
//	/**
//	 * Creates a `PipedInputStream` so
//	 * that it is not yet [ connected][.connect].
//	 * It must be [connected][PipedOutputStream.connect] to a
//	 * `PipedOutputStream` before being used.
//	 */
//	constructor() {
//		initPipe(DEFAULT_PIPE_SIZE)
//	}
//
//	/**
//	 * Creates a `PipedInputStream` so that it is not yet
//	 * [connected][.connect] and
//	 * uses the specified pipe size for the pipe's buffer.
//	 * It must be [ connected][PipedOutputStream.connect] to a `PipedOutputStream` before being used.
//	 *
//	 * @param      pipeSize the size of the pipe's buffer.
//	 * @exception  IllegalArgumentException if `pipeSize <= 0`.
//	 * @since      1.6
//	 */
//	constructor(pipeSize: Int) {
//		initPipe(pipeSize)
//	}

	/**
	 * Causes this piped input stream to be connected
	 * to the piped  output stream `src`.
	 * If this object is already connected to some
	 * other piped output  stream, an `IOException`
	 * is thrown.
	 *
	 *
	 * If `src` is an
	 * unconnected piped output stream and `snk`
	 * is an unconnected piped input stream, they
	 * may be connected by either the call:
	 *
	 * <pre>`snk.connect(src)` </pre>
	 *
	 *
	 * or the call:
	 *
	 * <pre>`src.connect(snk)` </pre>
	 *
	 *
	 * The two calls have the same effect.
	 *
	 * @param      src   The piped output stream to connect to.
	 * @exception  IOException  if an I/O error occurs.
	 */
//	@Throws(IOException::class)
//	fun connect(src: PipedOutputStream) {
//		src.connect(this)
//	}

	/**
	 * Receives a byte of data.  This method will block if no input is
	 * available.
	 * @param b the byte being received
	 * @exception IOException If the pipe is [ `broken`](#BROKEN),
	 * [unconnected][.connect],
	 * closed, or if an I/O error occurs.
	 * @since     1.1
	 */
//	@Synchronized
//	@Throws(IOException::class)
//	protected fun receive(b: Int) {
//		reportProgress(byteArrayOf((b and 0xFF).toByte()))
//		checkStateForReceive()
//		writeSide = Thread.currentThread()
//		if (`in` == out) awaitSpace()
//		if (`in` < 0) {
//			`in` = 0
//			out = 0
//		}
//		buffer!![`in`++] = (b and 0xFF).toByte()
//		if (`in` >= buffer!!.size) {
//			`in` = 0
//		}
//	}

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
	@Synchronized
	@Throws(IOException::class)
	fun promiseReceive(b: ByteArray, off: Int, len: Int): Promise<*> {
//		val transferArray = ByteArray(len)
//		System.arraycopy(b, off, transferArray, `in`, len)
//		reportProgress(transferArray)
		var off = off
		checkStateForReceive()
//		writeSide = Thread.currentThread()
		var bytesToTransfer = len
		return PromiseMachines.loop<Unit> { _, cancellable ->
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
//			if (bytesToTransfer <= 0) {
//				cancellable.cancel()
//				return@loop Unit.toPromise()
//			}

//			if (`in` == out) return@loop awaitSpace()
//
//			var nextTransferAmount = 0
//			if (out < `in`) {
//				nextTransferAmount = buffer.size - `in`
//			} else {
//				if (`in` == -1) {
//					out = 0
//					`in` = 0
//					nextTransferAmount = buffer.size - `in`
//				} else {
//					nextTransferAmount = out - `in`
//				}
//			}
//			if (nextTransferAmount > bytesToTransfer) nextTransferAmount = bytesToTransfer
//			assert(nextTransferAmount > 0)
//			b.copyInto(buffer, `in`, off, off + nextTransferAmount)
//			bytesToTransfer -= nextTransferAmount
//			off += nextTransferAmount
//			`in` += nextTransferAmount
//			if (`in` >= buffer.size) {
//				`in` = 0
//			}
//			Unit.toPromise()
		}
//		while (bytesToTransfer > 0) {
//			if (`in` == out) awaitSpace()
//			var nextTransferAmount = 0
//			if (out < `in`) {
//				nextTransferAmount = buffer.size - `in`
//			} else if (`in` < out) {
//				if (`in` == -1) {
//					out = 0
//					`in` = 0
//					nextTransferAmount = buffer.size - `in`
//				} else {
//					nextTransferAmount = out - `in`
//				}
//			}
//			if (nextTransferAmount > bytesToTransfer) nextTransferAmount = bytesToTransfer
//			assert(nextTransferAmount > 0)
//			b.copyInto(buffer, `in`, off, off + nextTransferAmount)
//			bytesToTransfer -= nextTransferAmount
//			off += nextTransferAmount
//			`in` += nextTransferAmount
//			if (`in` >= buffer.size) {
//				`in` = 0
//			}
//		}
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
//		while (`in` == out) {
//			checkStateForReceive()
//
//			/* full: kick any waiting readers */
//			(this as Object).notifyAll()
//			try {
//				(this as Object).wait(1000)
//			} catch (ex: InterruptedException) {
//				// Android-changed: re-set the thread's interrupt status
//				// throw new java.io.InterruptedIOException();
//				IoUtils.throwInterruptedIoException()
//			}
//		}
	}

	/**
	 * Notifies all waiting threads that the last byte of data has been
	 * received.
	 */
	fun receivedLast() {
		tryFeeding()
		closedByWriter = true
		close()
	}

	/**
	 * Reads the next byte of data from this piped input stream. The
	 * value byte is returned as an `int` in the range
	 * `0` to `255`.
	 * This method blocks until input data is available, the end of the
	 * stream is detected, or an exception is thrown.
	 *
	 * @return     the next byte of data, or `-1` if the end of the
	 * stream is reached.
	 * @exception  IOException  if the pipe is
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

//		var trials = 2
//		while (`in` < 0) {
//			if (closedByWriter) {
//				/* closed by writer, return EOF */
//				return (-1).toPromise()
//			}
//			if ((writeSide != null) && (!writeSide!!.isAlive()) && (--trials < 0)) {
//				throw IOException("Pipe broken")
//			}
//			/* might be a writer waiting */
//			(this as Object).notifyAll()
//			try {
//				(this as Object).wait(1000)
//			} catch (ex: InterruptedException) {
//				// Android-changed: re-set the thread's interrupt status
//				// throw new java.io.InterruptedIOException();
//				IoUtils.throwInterruptedIoException()
//			}
//		}


//		reaper.progress.then {  }
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
		/* possibly wait on the first character */
//		val c = promiseRead()
//		if (c < 0) {
//			return -1
//		}
//		b[off] = c.toByte()
//		var rlen = 1
//		while ((`in` >= 0) && (len > 1)) {
//			var available: Int
//
//			if (`in` > out) {
//				available = min((buffer!!.size - out), (`in` - out))
//			} else {
//				available = buffer!!.size - out
//			}
//
//			// A byte is read beforehand outside the loop
//			if (available > (len - 1)) {
//				available = len - 1
//			}
//			System.arraycopy(buffer, out, b, off + rlen, available)
//			out += available
//			rlen += available
//			len -= available
//
//			if (out >= buffer!!.size) {
//				out = 0
//			}
//			if (`in` == out) {
//				/* now empty */
//				`in` = -1
//			}
//		}
//		return rlen
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

	/**
	 * Closes this piped input stream and releases any system resources
	 * associated with the stream.
	 *
	 * @exception  IOException  if an I/O error occurs.
	 */
	@Throws(IOException::class)
	override fun close() {
		promiseClose()
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

//	private class Receiver: PromiseMachines.ContinuableMachine<ByteArray>() {
//		private var promisedBytes = ResolvedPromiseBox(ReceivedByteArrayPromise())
//
//		fun receive(byteArray: ByteArray) {
//
//		}
//
//		override fun next(): Promise<ByteArray> {}
//
//		private class ReceivedByteArrayPromise : Promise<ByteArray>() {
//			fun receive(byteArray: ByteArray) {
//				resolve(byteArray)
//			}
//		}
//	}

	private abstract class AbstractFeeder() : Promise<Int>() {
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

		init {
			awaitCancellation { resolve(read) }
		}

		override fun feed(byteArray: ByteArray, offset: Int, maxAmount: Int): Int =
			try {
				val amountToRead = (capacity - read).coerceAtMost(byteArray.size - offset).coerceAtMost(maxAmount)
				val endIndex = offset + amountToRead
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

	private class Sipper() : AbstractFeeder() {
		override val capacity: Int = 1

		init {
		    awaitCancellation { resolve(-1) }
		}

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
