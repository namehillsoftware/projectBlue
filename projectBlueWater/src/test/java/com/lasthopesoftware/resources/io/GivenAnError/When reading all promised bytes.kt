package com.lasthopesoftware.resources.io.GivenAnError

import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.closables.eventuallyUse
import com.lasthopesoftware.resources.io.PromisingChannel
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class `When reading all promised bytes` {

	private val bytes by lazy {
		"Lorem ipsum fusceefficitur.".toByteArray()
	}

	private val readBytes = ByteArray(bytes.size)
	private var cause: Exception? = null

	@BeforeAll
	fun act() {
		val pipingInputStream = PromisingChannel()
		val promisedBytes = pipingInputStream.eventuallyUse {
			Promise.whenAll(
				(0 until readBytes.size).map { off ->
					it.promiseRead(readBytes, off, 1)
				}
			)
		}

		pipingInputStream.writableStream.eventuallyUse { writer ->
			writer.promiseWrite(bytes, 0, bytes.size / 2)
				.then { writer.closeWithCause(Exception("'cause")) }
		}.toExpiringFuture().get()

		try {
			promisedBytes.toExpiringFuture().get()
		} catch (ee: ExecutionException) {
			cause = ee.cause as? Exception
		}
	}

	@Test
	fun `then the initial bytes are correct`() {
		assertThat(readBytes.takeWhile { it == 0.toByte() }).isEmpty()
	}

	@Test
	fun `then the initial read bytes are correct`() {
		assertThat(readBytes.filterNot { it == 0.toByte() })
			.isEqualTo(bytes.take(bytes.size / 2).toList())
	}

	@Test
	fun `then all read bytes are correct`() {
		assertThat(readBytes.toList())
			.isEqualTo(bytes.take(bytes.size / 2) + List<Byte>(bytes.size - bytes.size / 2) { 0 })
	}

	@Test
	fun `then the exception is correct`() {
		assertThat(cause?.message).isEqualTo("'cause")
	}
}
