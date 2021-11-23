
package com.lasthopesoftware.bluewater.shared.policies.ratelimiting

import com.lasthopesoftware.bluewater.shared.promises.NoopResponse.Companion.ignore
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class PromisingRateLimiter<T>(rate: Int): RateLimitPromises<T>, ImmediateAction {
	private val availablePromises = AtomicInteger(rate)
	private val queuedPromises = ConcurrentLinkedDeque<() -> Promise<T>>()

	override fun limit(factory: () -> Promise<T>): Promise<T> =
		Promise<T> { m ->
			val promiseProxy = PromiseProxy(m)
			// Use resolve/rejection handler over `must` so that errors don't propagate as unhandled
			queuedPromises.offer { factory().also(promiseProxy::proxy) }
			doNext()
		}

	private fun doNext() {
		val p = queuedPromises.poll() ?: return

		// Essentially getAndAccumulate from more recent versions of the JDK
		var prev: Int
		var next: Int
		do {
			prev = availablePromises.get()
			next = max(prev - 1, 0)
		} while (!availablePromises.compareAndSet(prev, next))

		if (prev == 0) {
			queuedPromises.push(p)
			return
		}

		p().ignore().must(this)
	}

	override fun act() {
		availablePromises.incrementAndGet()
		doNext()
	}
}
