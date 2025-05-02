package com.lasthopesoftware.bluewater.client.connection.polling.GivenStandardConnectionPollTimes

import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPollTimes
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When getting the poll times` {

	private var pollTimes = emptyList<Duration>()

	@BeforeAll fun act() {
		var previousDuration = Duration.ZERO
		pollTimes = ConnectionPollTimes.getConnectionTimes()
			.takeWhile {
				previousDuration != it
			}
			.onEach { previousDuration = it }
			.toList()
	}

	@Test fun `then the list is correct`() {
		assertThat(pollTimes).containsExactly(
			Duration.standardSeconds(1),
			Duration.standardSeconds(2),
			Duration.standardSeconds(4),
			Duration.standardSeconds(8),
			Duration.standardSeconds(16),
			Duration.standardSeconds(32),
		)
	}
}
