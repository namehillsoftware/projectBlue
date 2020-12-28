package com.lasthopesoftware.bluewater.shared.resilience.GivenAnArmedCircuitBreaker

import com.lasthopesoftware.bluewater.shared.resilience.CircuitBreaker
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test

class WhenTriggeringTheCircuitBreakerOneSecondLater {

	companion object {

		private var breakerTripped = false

		@JvmStatic
		@BeforeClass
		fun setup() {
			val circuitBreaker = CircuitBreaker(Duration.standardSeconds(2))

			breakerTripped = circuitBreaker.armBreaker()

			Thread.sleep(Duration.standardSeconds(1).millis)

			breakerTripped = circuitBreaker.armBreaker()
		}
	}

	@Test
	fun thenTheCircuitIsBroken() {
		assertThat(breakerTripped).isTrue
	}
}
