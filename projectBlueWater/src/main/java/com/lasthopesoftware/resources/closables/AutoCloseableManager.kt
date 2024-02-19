package com.lasthopesoftware.resources.closables

import com.lasthopesoftware.bluewater.shared.lazyLogger
import java.util.Stack

class AutoCloseableManager : ManageCloseables, AutoCloseable {

	companion object {
		private val logger by lazyLogger<AutoCloseableManager>()
	}

	private val lock = Any()
	private val closeables = Stack<AutoCloseable>()

	override fun <T : AutoCloseable> manage(closeable: T): T = synchronized(lock) {
		closeables.push(closeable)
		return closeable
	}

	override fun close() = synchronized(lock) {
		while (closeables.isNotEmpty()) {
			try {
				closeables.pop()?.close()
			} catch (e: Exception) {
				logger.warn("There was an error closing a resource", e)
			}
		}
	}
}
