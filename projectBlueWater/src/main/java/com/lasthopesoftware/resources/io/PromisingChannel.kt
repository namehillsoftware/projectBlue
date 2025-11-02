package com.lasthopesoftware.resources.io

import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.shared.drainQueue
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.guaranteedUnitResponse
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.io.PromisingReadableStream.Companion.readingCancelledException
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.Volatile

class PromisingChannel() : PromisingReadableStream {

	companion object {
		private val logger by lazyLogger<PromisingChannel>()

		private fun logDebug(message: String) {
			if (BuildConfig.DEBUG) {
				logger.debug(message)
			}
		}
	}

	private val consumers = ConcurrentLinkedQueue<Consumer>()
	private val servers = ConcurrentLinkedQueue<Server>()
	private val sync = Any()

	@Volatile
	private var isWriterClosed = false

	@Volatile
	private var isReaderClosed = false

	private val connectableWritableStream by lazy { ConnectedWritableStream() }

	val writableStream: PromisingWritableStream
		get() = connectableWritableStream

	val outputStream: OutputStream
		get() = connectableWritableStream

	override fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int> = when {
		isReaderClosed -> throw ChannelClosedException()
		off < 0 || len < 0 || len > b.size - off -> throw IndexOutOfBoundsException()
		len == 0 -> 0.toPromise()
		// Closed by writer and queue flushed
		isWriterClosed && servers.isEmpty() -> {
			(-1).toPromise()
		}
		else -> {
			val consumer = Consumer(b, off, len)
			consumers.offer(consumer)
			tryConsuming()
			consumer
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
		return servers.sumOf { servers -> servers.bytesToTransfer }
	}

	override fun promiseClose(): Promise<Unit> = synchronized(sync) {
		logDebug("Closing PromisingChannel for reading.")
		isReaderClosed = true
		if (consumers.isEmpty()) {
			for (server in servers.drainQueue())
				server.cancel()
			Unit.toPromise()
		} else {
			val openConsumers = consumers.toList()
			tryConsuming()
			Promise.whenAll(openConsumers).guaranteedUnitResponse()
		}
	}

	private fun tryConsuming(): Unit = synchronized(sync) {
		while (true) {
			val consumer = consumers.peek()
			if (consumer == null) break

			val server = servers.peek()
			if (server == null) {
				if (isWriterClosed) {
					// Drain consumers
					consumer.servingComplete()
					if (consumers.peek() == consumer)
						consumers.poll()
					continue
				}
				break
			}

			consumer.consume(server)

			if (consumer.isFed && consumers.peek() == consumer)
				consumers.poll()

			if (server.isEmpty && servers.peek() == server)
				servers.poll()
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
	private fun promiseReceive(b: ByteArray, off: Int, len: Int): Promise<Int> {
		if (isReaderClosed || isWriterClosed) return 0.toPromise()

		val server = Server(b, off, len)
		servers.offer(server)
		tryConsuming()
		return server
	}

	private fun receivedLast() {
		logDebug("Closing PromisingChannel for writing.")
		isWriterClosed = true
		tryConsuming()
	}

	private inner class ConnectedWritableStream() : OutputStream(), PromisingWritableStream,
		ImmediateResponse<Unit, ConnectedWritableStream> {
		override fun promiseWrite(buffer: ByteArray, offset: Int, length: Int): Promise<Int> =
			promiseReceive(buffer, offset, length)

		override fun promiseFlush(): Promise<Unit> {
			tryConsuming()
			return Promise.whenAll(servers.toList()).unitResponse()
		}

		override fun promiseClose(): Promise<Unit> = receivedLast().toPromise()

		override fun respond(resolution: Unit?): ConnectedWritableStream = this

		override fun write(b: Int) {
			write(byteArrayOf(b.toByte()), 0, 1)
		}

		override fun write(b: ByteArray, off: Int, len: Int) {
			promiseReceive(b, off, len)
		}

		override fun close() {
			receivedLast()
		}
	}

	private class Server(
		private val bytes: ByteArray,
		@Volatile private var offset: Int,
		bytesToTransfer: Int
	) : Promise<Int>(), CancellationResponse {
		private val sync = Any()
		private val startingOffset = offset

		@Volatile
		private var isCancelled = false

		@Volatile
		var bytesToTransfer = bytesToTransfer
			private set

		val isEmpty: Boolean
			get() = isCancelled || bytesToTransfer <= 0

		init {
			awaitCancellation(this)
		}

		override fun cancellationRequested() {
			isCancelled = true
			resolve(offset - startingOffset)
		}

		fun serve(destination: ByteArray, destinationOffset: Int, nextTransferAmount: Int): Int = synchronized(sync) {
			if (isEmpty) return 0

			val nextTransferAmount = nextTransferAmount.coerceAtMost(bytesToTransfer)
			if (nextTransferAmount < 0) return 0

			bytes.copyInto(destination, destinationOffset, offset, offset + nextTransferAmount)
			bytesToTransfer -= nextTransferAmount
			offset += nextTransferAmount

			if (isEmpty)
				resolve(offset - startingOffset)

			return nextTransferAmount
		}
	}

	private inner class Consumer(
		private val destination: ByteArray,
		private val destinationOffset: Int,
		val capacity: Int,
	) : Promise<Int>(), CancellationResponse {

		private val sync = Any()

		@Volatile
		private var read = 0

		@Volatile
		private var isCancelled = false

		val isFed
			get() = isCancelled || read >= capacity

		init {
		    awaitCancellation(this)
		}

		override fun cancellationRequested() {
			isCancelled = true
			synchronized(sync) {
				reject(readingCancelledException())
			}
		}

		fun servingComplete() {
			synchronized(sync) {
				resolve(read)
			}
		}

		fun consume(server: Server): Int {
			try {
				if (isFed) return 0

				synchronized(sync) {
					if (isFed) return 0

					val offset = destinationOffset + read
					val fed = server.serve(destination, offset, capacity - read)
					read += fed

					if (isFed)
						resolve(read)

					return fed
				}
			} catch (e: Throwable) {
				reject(e)
				throw e
			}
		}
	}

	class ChannelClosedException() : IOException("Channel Closed")
}
