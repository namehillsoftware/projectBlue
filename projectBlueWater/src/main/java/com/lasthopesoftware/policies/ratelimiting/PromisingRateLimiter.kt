
package com.lasthopesoftware.policies.ratelimiting

import com.lasthopesoftware.promises.NoopResponse.Companion.ignore
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.ProgressingPromiseProxy
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

class PromisingRateLimiter<T>(private val rate: Int): RateLimitPromises<T>, ImmediateAction {
	private val availablePromises = AtomicInteger(rate)
	private val queuedPromises = ConcurrentLinkedQueue<() -> Promise<T>>()

	override fun enqueuePromise(factory: () -> Promise<T>): Promise<T> =
		object : Promise.Proxy<T>() {
			init {
				queuedPromises.offer { factory().also(::proxy) }
				doNext()
			}
		}

	override fun <Progress> enqueueProgressingPromise(factory: () -> ProgressingPromise<Progress, T>): ProgressingPromise<Progress, T> =
		object : ProgressingPromiseProxy<Progress, T>() {
			init {
				queuedPromises.offer { factory().also(::proxy) }
				doNext()
			}
		}

	private fun doNext() {
		while (true) {
			// Essentially getAndAccumulate from more recent versions of the JDK
			var prev: Int
			do {
				// Drain the queue or max out number of open promises
				if (queuedPromises.isEmpty()) return
				prev = availablePromises.get()
				val next = max(prev - 1, 0)
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
		do {
			val prev = availablePromises.get()
			val next = min(prev + 1, rate)
		} while (!availablePromises.compareAndSet(prev, next))
	}
}
