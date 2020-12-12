package com.lasthopesoftware.resources.closables

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue

class CloseableManager : ManageCloseables {

	companion object {
		private val logger = LoggerFactory.getLogger(CloseableManager::class.java)
	}

	private val closeables = ConcurrentLinkedQueue<AutoCloseable>()

	override fun manage(closeable: AutoCloseable) {
		closeables.offer(closeable)
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
