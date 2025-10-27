package com.lasthopesoftware.resources.io.GivenBytes

import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.io.PipedPromisingInputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Random

class `When reading the promised bytes` {

	companion object {
		private const val byteArraySize = 2116
		private const val chunkSize = 361
	}

	private val bytes by lazy {
		val bytes = ByteArray(byteArraySize)
		Random().nextBytes(bytes)
		bytes
	}

	private val readBytes = ByteArray(2300)

	@BeforeAll
	fun act() {
		val pipingInputStream = PipedPromisingInputStream()
		val promisedRead = pipingInputStream
			.promiseRead(readBytes, 100, 2100)
			.inevitably { pipingInputStream.promiseClose() }

		for (i in 0 until bytes.size step chunkSize) {
			val len = (bytes.size - i).coerceAtMost(chunkSize)
			if (len <= 0) break
			pipingInputStream.promiseReceive(bytes, i, len).toExpiringFuture().get()
		}

		promisedRead.toExpiringFuture().get()
	}

	@Test
	fun `then the initial bytes are correct`() {
		assertThat(readBytes.takeWhile { it == 0.toByte() }).hasSize(100)
	}

	@Test
	fun `then the read bytes are correct`() {
		assertThat(readBytes.dropWhile { it == 0.toByte() }.take(chunkSize))
			.isEqualTo(bytes.dropWhile { it == 0.toByte() }.take(chunkSize))
	}
}
