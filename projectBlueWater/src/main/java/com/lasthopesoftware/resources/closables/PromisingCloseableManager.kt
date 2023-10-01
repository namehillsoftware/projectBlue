package com.lasthopesoftware.resources.closables

import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentLinkedQueue

class PromisingCloseableManager : ManagePromisingCloseables, PromisingCloseable {

	companion object {
		private val logger by lazyLogger<PromisingCloseableManager>()
	}

	private val closeables = ConcurrentLinkedQueue<PromisingCloseable>()

	override fun <T : PromisingCloseable> manage(closeable: T): T {
		closeables.offer(closeable)
		return closeable
	}

	override fun promiseClose(): Promise<Unit> {
		return if (closeables.isEmpty()) Unit.toPromise()
		else closeables
			.poll()
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
