package com.lasthopesoftware.resources.io.GivenALargeResponse

import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.closables.eventuallyUse
import com.lasthopesoftware.resources.closables.thenUse
import com.lasthopesoftware.resources.io.PromisingChannel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When reading all promised bytes` {

	private val bytes by lazy {
		javaClass.getResourceAsStream("large-response.txt")!!.readAllBytes()
	}

	private var readBytes: ByteArray? = null

	@BeforeAll
	fun act() {
		val pipingInputStream = PromisingChannel()
		val promisedBytes = pipingInputStream.eventuallyUse { it.promiseReadAllBytes() }

		pipingInputStream.writableStream.thenUse {
			for (i in 0 until bytes.size step 8192)
				it.promiseWrite(bytes, i, 8192.coerceAtMost(bytes.size - i)).toExpiringFuture().get()
		}

		readBytes = promisedBytes.toExpiringFuture().get()
	}

	@Test
	fun `then the initial bytes are correct`() {
		assertThat(readBytes?.takeWhile { it == 0.toByte() }).isEmpty()
	}

	@Test
	fun `then all read bytes are correct`() {
		assertThat(readBytes?.filterNot { it == 0.toByte() })
			.isEqualTo(bytes.dropWhile { it == 0.toByte() })
	}
}
