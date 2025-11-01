package com.lasthopesoftware.resources.io

import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.guaranteedUnitResponse
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.Volatile

class PromisingChannel() : PromisingReadableStream {

	companion object {
		private val logger by lazyLogger<PromisingChannel>()
	}

	private val consumers = ConcurrentLinkedQueue<Consumer>()
	private val servers = ConcurrentLinkedQueue<Server>()
	private val sync = Any()

	@Volatile
	private var writerClosed = false

	@Volatile
	private var readerClosed = false

	val writableStream: OutputStream by lazy { ConnectedWritableStream() }

	override fun promiseRead(b: ByteArray, off: Int, len: Int): Promise<Int> = when {
		readerClosed -> throw IOException("Pipe closed")
		off < 0 || len < 0 || len > b.size - off -> throw IndexOutOfBoundsException()
		len == 0 -> 0.toPromise()
		// Closed by writer and queue flushed
		writerClosed && servers.isEmpty() -> {
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

	override fun promiseClose(): Promise<Unit> {
		logger.debug("Closing PromisingChannel for reading.")
		readerClosed = true
		tryConsuming()
		return Promise.whenAll(consumers.toList()).guaranteedUnitResponse()
	}

	private fun tryConsuming(): Unit = synchronized(sync) {
		while (true) {
			val consumer = consumers.peek()
			if (consumer == null) break

			val server = servers.peek()
			if (server == null) {
				if (writerClosed) {
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
	private fun receive(b: ByteArray, off: Int, len: Int) {
		checkStateForReceive()
//		synchronized(sync) {
			servers.offer(Server(b, off, len))
			tryConsuming()
//		}
	}

	private fun checkStateForReceive() {
		if (writerClosed || readerClosed) {
			throw IOException("Pipe closed")
		}
	}

	/**
	 * Notifies all waiting threads that the last byte of data has been
	 * received.
	 */
	private fun receivedLast() {
		logger.debug("Closing PromisingChannel for writing.")
		writerClosed = true
		tryConsuming()
	}

	private inner class ConnectedWritableStream() : OutputStream() {

		override fun write(b: Int) {
			write(byteArrayOf(b.toByte()), 0, 1)
		}

		override fun write(b: ByteArray, off: Int, len: Int) {
			receive(b, off, len)
		}

		override fun close() {
			receivedLast()
		}
	}

	private class Server(
		private val bytes: ByteArray,
		@Volatile private var offset: Int,
		bytesToTransfer: Int
	) {
		private val sync = Any()

		@Volatile
		var bytesToTransfer = bytesToTransfer
			private set

		val isEmpty: Boolean
			get() = bytesToTransfer <= 0

		fun serve(destination: ByteArray, destinationOffset: Int, nextTransferAmount: Int): Int = synchronized(sync) {
			val nextTransferAmount = nextTransferAmount.coerceAtMost(bytesToTransfer)
			if (nextTransferAmount < 0) return 0

			bytes.copyInto(destination, destinationOffset, offset, offset + nextTransferAmount)
			bytesToTransfer -= nextTransferAmount
			offset += nextTransferAmount

			return nextTransferAmount
		}
	}

//	private abstract class AbstractConsumer() : Promise<Int>(), CancellationResponse {
//		init {
//			awaitCancellation(this)
//		}
//
//		override fun cancellationRequested() {
//			resolve(0)
//		}
//
//		abstract val isFed: Boolean
//		abstract val capacity: Int
//		abstract fun consume(server: Server): Int
//	}

	private class Consumer(
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
				resolve(read)
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
}
