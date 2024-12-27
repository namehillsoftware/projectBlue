package com.lasthopesoftware.bluewater.client.connection.retries.GivenAPromiseRejectsWithAnIoCanceledErrorRepeatedly

import com.lasthopesoftware.bluewater.client.connection.ConnectionLostRetryHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.policies.retries.RecursivePromiseRetryHandler
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Period
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

class `When handling it with the ConnectionLostRetryHandler` {

	private var attempts = 0
	private var exception: IOException? = null
	private var executionTime: Duration? = null

	@BeforeAll
	fun act() {
		val now = DateTime.now()
		try {
			ConnectionLostRetryHandler(RecursivePromiseRetryHandler).retryOnException<Unit> {
				++attempts
				Promise(IOException("canceled"))
			}.toExpiringFuture().get()
		} catch (ee: ExecutionException) {
			exception = ee.cause as? IOException
		} finally {
			executionTime = Period(now, DateTime.now()).toStandardDuration()
		}
	}

	@Test
	fun `then 3 attempts are made to resolve the issue`() {
		assertThat(attempts).isEqualTo(3)
	}

	@Test
	fun `then the retries take the correct amount of time`() {
		assertThat(executionTime?.millis).isGreaterThanOrEqualTo(Duration.standardSeconds(15).millis)
	}

	@Test
	fun `then the error is caught`() {
		assertThat(exception).isNotNull()
	}
}
