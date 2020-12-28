package com.lasthopesoftware.bluewater.shared.resilience.GivenATwoSecondTwoTriggersTimedCountdownLatch

import com.lasthopesoftware.bluewater.shared.resilience.TimedCountdownLatch
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test

class WhenTriggeringTheLatch {

	companion object {

		private var isClosed = false

		@JvmStatic
		@BeforeClass
		fun setup() {
			val timedLatch = TimedCountdownLatch(2, Duration.standardSeconds(2))

			isClosed = timedLatch.trigger()
		}
	}

	@Test
	fun thenTheLatchIsNotClosed() {
		assertThat(isClosed).isFalse
	}
}
