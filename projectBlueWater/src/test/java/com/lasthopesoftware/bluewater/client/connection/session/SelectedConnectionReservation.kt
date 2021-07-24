package com.lasthopesoftware.bluewater.client.connection.session

import java.util.concurrent.locks.ReentrantLock

class SelectedConnectionReservation : AutoCloseable {
	companion object {
		private val lock = ReentrantLock()
	}

	init { lock.lock() }

	override fun close() = lock.unlock()
}
