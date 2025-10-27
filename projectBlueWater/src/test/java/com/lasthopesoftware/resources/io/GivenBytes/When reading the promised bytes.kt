package com.lasthopesoftware.resources.io.GivenBytes

import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.closables.thenUse
import com.lasthopesoftware.resources.io.PromisingChannel
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

	private val readBytes = ByteArray(100)

	@BeforeAll
	fun act() {
		val pipingInputStream = PromisingChannel(4)
		val promisedRead = pipingInputStream
			.promiseRead(readBytes, 1, 20)
			.inevitably { pipingInputStream.promiseClose() }

		pipingInputStream.writableStream.thenUse { os ->
			for (i in 0 until bytes.size step chunkSize) {
				val len = (bytes.size - i).coerceAtMost(chunkSize)
				if (len <= 0) break
				os.promiseWrite(bytes, i, len).toExpiringFuture().get()
			}
			os.flush().toExpiringFuture().get()
		}

		promisedRead.toExpiringFuture().get()
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
