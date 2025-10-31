package com.lasthopesoftware.resources.io.GivenAMultipleOfPipeSizeBytes

import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.closables.eventuallyUse
import com.lasthopesoftware.resources.io.PromisingChannel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When reading all promised bytes` {

	private val bytes by lazy {
		byteArrayOf(919.toByte(), 141.toByte(), 160.toByte(), 568.toByte(), 919.toByte(), 141.toByte(), 160.toByte(), 568.toByte(), )
	}

	private var readBytes: ByteArray = ByteArray(8)

	@BeforeAll
	fun act() {
		val pipingInputStream = PromisingChannel(2)
		val promisedBytes = pipingInputStream.eventuallyUse { it.promiseRead(readBytes, 0, 8) }

		pipingInputStream.writableStream.eventuallyUse {
			it.promiseWrite(bytes, 0, bytes.size)
		}.toExpiringFuture().get()

		promisedBytes.toExpiringFuture().get()
	}

	@Test
	fun `then the initial bytes are correct`() {
		assertThat(readBytes.takeWhile { it == 0.toByte() }).isEmpty()
	}

	@Test
	fun `then the initial read bytes are correct`() {
		assertThat(readBytes.dropWhile { it == 0.toByte() })
			.isEqualTo(bytes.dropWhile { it == 0.toByte() })
	}

	@Test
	fun `then all read bytes are correct`() {
		assertThat(readBytes.filterNot { it == 0.toByte() })
			.isEqualTo(bytes.dropWhile { it == 0.toByte() })
	}
}
