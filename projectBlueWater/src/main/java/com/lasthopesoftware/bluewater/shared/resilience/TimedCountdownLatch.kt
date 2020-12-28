package com.lasthopesoftware.bluewater.shared.resilience

import org.joda.time.Duration

class TimedCountdownLatch(private val triggers: Int, disarmDuration: Duration) {

	private val disarmDuration = disarmDuration.millis
	private var lastTriggered = 0L

	@Synchronized
	fun trigger(): Boolean {
		val triggerTime = System.currentTimeMillis()
		return (triggerTime - lastTriggered < disarmDuration).apply { lastTriggered = triggerTime }
	}
}
