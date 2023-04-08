package com.lasthopesoftware.resources.closables

import com.lasthopesoftware.bluewater.shared.lazyLogger
import java.util.concurrent.ConcurrentLinkedQueue

class AutoCloseableManager : ManageCloseables, AutoCloseable {

	companion object {
		private val logger by lazyLogger<AutoCloseableManager>()
	}

	private val closeables = ConcurrentLinkedQueue<AutoCloseable>()

	override fun <T : AutoCloseable> manage(closeable: T): T {
		closeables.offer(closeable)
		return closeable
	}

	override fun close() {
		while (closeables.isNotEmpty()) {
			try {
				closeables.poll()?.close()
			} catch (e: Exception) {
				logger.warn("There was an error closing a resource", e)
			}
		}
	}
}
