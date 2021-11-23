
package com.lasthopesoftware.bluewater.shared.policies.ratelimiting

import com.lasthopesoftware.bluewater.shared.promises.NoopResponse.Companion.ignore
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference

class PromisingRateLimiter<T>(rate: Int): RateLimitPromises<T> {
	private val activePromises = List(rate) { AtomicReference<() -> Promise<Unit>>() }
	private val queuedPromises = ConcurrentLinkedDeque<() -> Promise<Unit>>()

	override fun limit(factory: () -> Promise<T>): Promise<T> =
		Promise<T> { m ->
			val promiseProxy = PromiseProxy(m)
			// Use resolve/rejection handler over `must` so that errors don't propagate as unhandled
			queuedPromises.offer { factory().also(promiseProxy::proxy).unitResponse() }
			doNext()
		}

	private fun doNext() {
		val p = queuedPromises.poll() ?: return

		val reference = activePromises.firstOrNull { it.compareAndSet(null, p) }
		if (reference == null) {
			queuedPromises.push(p)
			return
		}

		p().ignore().must {
			reference.compareAndSet(p, null)
			doNext()
		}
	}
}
