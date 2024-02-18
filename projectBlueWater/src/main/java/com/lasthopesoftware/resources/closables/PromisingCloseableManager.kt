package com.lasthopesoftware.resources.closables

import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentLinkedDeque

class PromisingCloseableManager : ManagePromisingCloseables, PromisingCloseable {

	companion object {
		private val logger by lazyLogger<PromisingCloseableManager>()
	}

	private class AutoCloseableWrapper(private val closeable: AutoCloseable) : PromisingCloseable {
		override fun promiseClose(): Promise<Unit> {
			closeable.close()
			return Unit.toPromise()
		}
	}

	private val closeables = ConcurrentLinkedDeque<PromisingCloseable>()

	override fun <T : PromisingCloseable> manage(closeable: T): T {
		closeables.push(closeable)
		return closeable
	}

	override fun <T : AutoCloseable> manage(closeable: T): T {
		manage(AutoCloseableWrapper(closeable))
		return closeable
	}

	override fun promiseClose(): Promise<Unit> {
		return if (closeables.isEmpty()) Unit.toPromise()
		else closeables
			.pop()
			?.promiseClose()
			?.eventually(
				{ promiseClose() },
				{ e ->
					logger.warn("There was an error closing a resource", e)
					promiseClose()
				})
			.keepPromise(Unit)
	}
}
