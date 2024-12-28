package com.lasthopesoftware.bluewater.client.connection.retries.GivenAPromiseRejectsWithAParsingError

import com.lasthopesoftware.bluewater.client.connection.ConnectionLostRetryHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.policies.retries.ExecutedPromiseRetryHandler
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.text.ParseException
import java.util.concurrent.ExecutionException

class `When handling it with the ConnectionLostRetryHandler` {

	private var exception: ParseException? = null

	@BeforeAll
	fun act() {
		try {
			ConnectionLostRetryHandler(ExecutedPromiseRetryHandler).retryOnException<Unit> {
				Promise(ParseException("qPfvixjb", 13))
			}.toExpiringFuture().get()
		} catch (ee: ExecutionException) {
			exception = ee.cause as? ParseException
		}
	}

	@Test
	fun `then the error is caught`() {
		assertThat(exception?.message).isEqualTo("qPfvixjb")
	}
}
