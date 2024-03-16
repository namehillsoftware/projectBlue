
package com.lasthopesoftware.policies.ratelimiting

import com.lasthopesoftware.promises.NoopResponse.Companion.ignore
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

class PromisingRateLimiter<T>(private val rate: Int): RateLimitPromises<T>, ImmediateAction {
	private val availablePromises = AtomicInteger(rate)
	private val queuedPromises = ConcurrentLinkedQueue<() -> Promise<T>>()

	override fun limit(factory: () -> Promise<T>): Promise<T> =
		object : Promise.Proxy<T>() {
			init {
				// Use resolve/rejection handler over `must` so that errors don't propagate as unhandled
				queuedPromises.offer { factory().also(::proxy) }
				doNext()
			}
		}

	private fun doNext() {
		while (true) {
			// Essentially getAndAccumulate from more recent versions of the JDK
			var prev: Int
			var next: Int
			do {
				// Drain the queue or max out number of open promises
				if (queuedPromises.isEmpty()) return
				prev = availablePromises.get()
				next = max(prev - 1, 0)
			} while (!availablePromises.compareAndSet(prev, next))

			if (prev == 0) return

			val p = queuedPromises.poll()
			if (p != null) p().ignore().must(this)
			else makePromiseAvailable()
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
