package com.lasthopesoftware.bluewater.client.connection.retries.GivenAPromiseRejectsWithAnIoCanceledErrorRepeatedly.AndTheActualPromiseIsCancelled

import com.lasthopesoftware.bluewater.client.connection.ConnectionLostRetryHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

class `When handling it with the ConnectionLostRetryHandler` {

	private var attempts = 0
	private var exception: IOException? = null

	@BeforeAll
	fun act() {
		try {
			val deferredPromise = DeferredPromise(Unit)

			val retryingPromise = ConnectionLostRetryHandler.retryOnException<Unit> {
				++attempts
				deferredPromise
					.then { _ ->
						throw IOException("canceled")
					}
			}

			retryingPromise.cancel()

			deferredPromise.resolve()

			retryingPromise.toExpiringFuture().get()
		} catch (ee: ExecutionException) {
			exception = ee.cause as? IOException
		}
	}

	@Test
	fun `then the error is caught`() {
		assertThat(exception?.message).isEqualTo("canceled")
	}

	@Test
	fun `then the correct number of attempts are made`() {
		assertThat(attempts).isEqualTo(1)
	}
}
