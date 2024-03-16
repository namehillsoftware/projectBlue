package com.lasthopesoftware.bluewater.client.connection.retries.GivenAPromiseResolvesImmediately

import com.lasthopesoftware.bluewater.client.connection.ConnectionLostRetryHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When handling it with the ConnectionLostRetryHandler` {

	private var attempts = 0
	private var result: String? = null

	@BeforeAll
	fun act() {
		result = ConnectionLostRetryHandler.retryOnException {
			++attempts
			Promise("FQZPx0H")
		}.toExpiringFuture().get()
	}

	@Test
	fun `then the correct number of attempts are made`() {
		assertThat(attempts).isEqualTo(1)
	}

	@Test
	fun `then the result is correct`() {
		assertThat(result).isEqualTo("FQZPx0H")
	}
}
