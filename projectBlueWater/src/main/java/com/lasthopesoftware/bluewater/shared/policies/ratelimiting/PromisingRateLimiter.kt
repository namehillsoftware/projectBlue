
package com.lasthopesoftware.bluewater.shared.policies.ratelimiting

import com.lasthopesoftware.bluewater.shared.promises.NoopResponse.Companion.ignore
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

class PromisingRateLimiter<T>(private val rate: Int): RateLimitPromises<T>, ImmediateAction {
	private val availablePromises = AtomicInteger(rate)
	private val queuedPromises = ConcurrentLinkedQueue<() -> Promise<T>>()

	override fun limit(factory: () -> Promise<T>): Promise<T> =
		Promise<T> { m ->
			val promiseProxy = PromiseProxy(m)
			// Use resolve/rejection handler over `must` so that errors don't propagate as unhandled
			queuedPromises.offer { factory().also(promiseProxy::proxy) }
			doNext()
		}

	private fun doNext() {
		// Drain the queue or max out number of open promises
		while (availablePromises.get() > 0 && !queuedPromises.isEmpty()) {
			// Essentially getAndAccumulate from more recent versions of the JDK
			var prev: Int
			var next: Int
			do {
				if (queuedPromises.isEmpty()) return
				prev = availablePromises.get()
				next = max(prev - 1, 0)
			} while (!availablePromises.compareAndSet(prev, next))

			if (prev == 0) return

			val p = queuedPromises.poll()
			if (p == null) {
				makePromiseAvailable()
				return
			}

			p().ignore().must(this)
		}
	}

	override fun act() {
		makePromiseAvailable()
		doNext()
	}

	private fun makePromiseAvailable() {
		var prev: Int
		var next: Int
		do {
			prev = availablePromises.get()
			next = min(prev + 1, rate)
		} while (!availablePromises.compareAndSet(prev, next))
	}
}
