package com.lasthopesoftware.policies.retries.GivenAPromiseThatErrors

import com.lasthopesoftware.policies.retries.RecursivePromiseRetryHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class `When The Same Error Is Returned` {

	private var exception: Exception? = null

	@BeforeAll
	fun act() {
		try {
			RecursivePromiseRetryHandler.retryOnException<Unit> { e ->
				Promise(e ?: Exception("newb"))
			}.toExpiringFuture().get()
		} catch (ee: ExecutionException) {
			exception = ee.cause as? Exception
		}
	}

	@Test
	fun `then the error is caught`() {
		assertThat(exception?.message).isEqualTo("newb")
	}
}
