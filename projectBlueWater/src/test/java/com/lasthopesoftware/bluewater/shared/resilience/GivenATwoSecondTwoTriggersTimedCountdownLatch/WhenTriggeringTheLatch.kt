package com.lasthopesoftware.bluewater.shared.resilience.GivenATwoSecondTwoTriggersTimedCountdownLatch

import com.lasthopesoftware.bluewater.shared.resilience.TimedCountdownLatch
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenTriggeringTheLatch {

	private var isClosed = false

	@BeforeAll
	fun act() {
		val timedLatch = TimedCountdownLatch(2, Duration.standardSeconds(2))

		isClosed = timedLatch.trigger()
	}

	@Test
	fun `then the latch is not closed`() {
		assertThat(isClosed).isFalse
	}
}
