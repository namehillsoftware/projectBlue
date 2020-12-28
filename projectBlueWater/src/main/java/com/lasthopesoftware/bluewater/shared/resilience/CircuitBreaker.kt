package com.lasthopesoftware.bluewater.shared.resilience

import org.joda.time.Duration

class CircuitBreaker(private val disarmDuration: Duration) {

	fun armBreaker(): Boolean {
		return true
	}
}
