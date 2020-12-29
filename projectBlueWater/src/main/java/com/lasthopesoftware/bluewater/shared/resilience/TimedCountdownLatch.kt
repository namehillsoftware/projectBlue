package com.lasthopesoftware.bluewater.shared.resilience

import org.joda.time.Duration

class TimedCountdownLatch(private val maxTriggers: Int, disarmDuration: Duration) {

	private val disarmDuration = disarmDuration.millis
	private var lastTriggered = System.currentTimeMillis()
	private var triggers = 0

	@Synchronized
	fun trigger(): Boolean {
		val triggerTime = System.currentTimeMillis()
		// Open latch if more than max number of triggers has occurred
		if (++triggers >= maxTriggers) {
			// and the last trigger time is less than the disarm duration
			if (triggerTime - lastTriggered <= disarmDuration) {
				return true
			}

			// reset the trigger count if enough time has elapsed to reset the error count
			triggers = 0
		}
		lastTriggered = triggerTime
		return false
	}
}
