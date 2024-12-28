package com.lasthopesoftware.bluewater.client.connection.retries.GivenAPromiseRejectsWithASocketException.AndTheRetryHandlerIsCancelledBeforeNextAttempt

import com.lasthopesoftware.bluewater.client.connection.ConnectionLostRetryHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.policies.retries.ExecutedPromiseRetryHandler
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.SocketException
import java.util.concurrent.ExecutionException

class `When handling it with the ConnectionLostRetryHandler` {

	private var attempts = 0
	private var exception: SocketException? = null

	@BeforeAll
	fun act() {
		try {
			val deferredPromise = DeferredPromise(Unit)

			val retryingPromise = ConnectionLostRetryHandler(ExecutedPromiseRetryHandler).retryOnException {
				++attempts
				if (attempts == 2) deferredPromise.resolve()

				Promise<Unit>(SocketException())
			}

			deferredPromise.then { _ -> retryingPromise.cancel() }

			retryingPromise.toExpiringFuture().get()
		} catch (ee: ExecutionException) {
			exception = ee.cause as? SocketException
		}
	}

	@Test
	fun `then the error is caught`() {
		assertThat(exception).isNotNull()
	}

	@Test
	fun `then the correct number of attempts are made`() {
		assertThat(attempts).isEqualTo(2)
	}
}
