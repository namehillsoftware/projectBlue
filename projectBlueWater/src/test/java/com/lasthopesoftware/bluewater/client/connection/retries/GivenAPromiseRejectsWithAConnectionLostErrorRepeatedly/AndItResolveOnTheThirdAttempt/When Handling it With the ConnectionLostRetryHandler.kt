package com.lasthopesoftware.bluewater.client.connection.retries.GivenAPromiseRejectsWithAConnectionLostErrorRepeatedly.AndItResolveOnTheThirdAttempt

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
import java.net.UnknownHostException

class `When handling it with the ConnectionLostRetryHandler` {

	private var attempts = 0
	private var executionTime: Duration? = null
	private var result: String? = null

	@BeforeAll
	fun act() {
		val now = DateTime.now()
		try {
			result = ConnectionLostRetryHandler(RecursivePromiseRetryHandler).retryOnException {
				++attempts
				if (attempts < 3) Promise(UnknownHostException("who?")) else Promise("rtJcqvk")
			}.toExpiringFuture().get()
		} finally {
			executionTime = Period(now, DateTime.now()).toStandardDuration()
		}
	}

	@Test
	fun `then the correct number of attempts are made`() {
		assertThat(attempts).isEqualTo(3)
	}

	@Test
	fun `then the retries take the correct amount of time`() {
		assertThat(executionTime?.millis).isGreaterThanOrEqualTo(Duration.standardSeconds(15).millis)
	}

	@Test
	fun `then the result is correct`() {
		assertThat(result).isEqualTo("rtJcqvk")
	}
}
