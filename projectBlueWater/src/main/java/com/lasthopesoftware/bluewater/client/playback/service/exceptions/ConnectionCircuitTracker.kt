package com.lasthopesoftware.bluewater.client.playback.service.exceptions

import org.slf4j.LoggerFactory

class ConnectionCircuitTracker : BreakConnection {

	private companion object {
		private val logger = LoggerFactory.getLogger(ConnectionCircuitTracker::class.java)!!
		private const val maxErrors = 3
		private const val errorCountResetDuration = 1000
	}

	private var numberOfErrors = 0
	private var lastErrorTime: Long = 0

	override fun isConnectionPastThreshold(): Boolean {
		val currentErrorTime = System.currentTimeMillis()
		// Stop handling errors if more than the max errors has occurred
		if (++numberOfErrors > maxErrors) {
			// and the last error time is less than the error count reset duration
			if (currentErrorTime <= lastErrorTime + errorCountResetDuration) {
				logger.warn("Number of errors has not surpassed " + maxErrors + " in less than " + errorCountResetDuration + "ms. Closing and restarting playlist manager.")
				return false
			}

			// reset the error count if enough time has elapsed to reset the error count
			numberOfErrors = 0
		}
		lastErrorTime = currentErrorTime
		return true
	}
}
