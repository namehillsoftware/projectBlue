package com.lasthopesoftware.resources.io.GivenBytes

import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.eventuallyUse
import com.lasthopesoftware.resources.io.PromisingChannel
import com.lasthopesoftware.resources.io.PromisingWritableStream
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When reading the promised bytes` {

	companion object {
		private const val chunkSize = 3
	}

	private val bytes by lazy {
		"Hello there".toByteArray()
	}

	fun PromisingWritableStream.writeChunk(chunkIndex: Int): Promise<Int> {
		val chunkOffset = chunkSize * chunkIndex
		val len = (bytes.size - chunkOffset).coerceAtMost(chunkSize)
		if (len <= 0) return 0.toPromise()
		return promiseWrite(bytes, chunkOffset, len)
	}

	private var postClosedBytes = 0

	private val readBytes = ByteArray(100)

	@BeforeAll
	fun act() {
		val pipingInputStream = PromisingChannel()
		pipingInputStream.eventuallyUse {
			var promisedRead: Promise<Int> = 0.toPromise()

			pipingInputStream.writableStream.eventuallyUse { os ->

				os.writeChunk(0)
				os.writeChunk(1)

				promisedRead = pipingInputStream.promiseRead(readBytes, 1, 20)

				os.writeChunk(2)
				os.writeChunk(3)
				os.promiseFlush()
			}.eventually { promisedRead }
		}.toExpiringFuture().get()

		postClosedBytes = pipingInputStream.writableStream.promiseWrite(byteArrayOf(35.toByte()), 0, 1).toExpiringFuture().get() ?: 0
	}

	@Test
	fun `then no bytes are written after the channel is closed`() {
		assertThat(postClosedBytes).isZero
	}

	@Test
	fun `then the initial bytes are correct`() {
		assertThat(readBytes.takeWhile { it == 0.toByte() }).hasSize(1)
	}

	@Test
	fun `then the initial read bytes are correct`() {
		assertThat(readBytes.dropWhile { it == 0.toByte() }.take(chunkSize))
			.isEqualTo(bytes.dropWhile { it == 0.toByte() }.take(chunkSize))
	}

	@Test
	fun `then all read bytes are correct`() {
		assertThat(readBytes.filterNot { it == 0.toByte() })
			.isEqualTo(bytes.dropWhile { it == 0.toByte() })
	}
}
