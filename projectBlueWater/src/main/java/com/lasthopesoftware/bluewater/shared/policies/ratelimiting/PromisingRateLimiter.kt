
package com.lasthopesoftware.bluewater.shared.policies.ratelimiting

import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max

class PromisingRateLimiter<T>(private val rate: Int): RateLimitPromises<T> {
	private val activePromiseSync = Any()
	@Volatile
	private var activePromises = 0
	private val queuedPromises = ConcurrentLinkedQueue<() -> Promise<Unit>>()
	private val resolutionHandler = PromisedResponse<T, Unit> { finishCurrentDoNext() }
	private val rejectionHandler = PromisedResponse<Throwable, Unit> { finishCurrentDoNext() }

	override fun limit(factory: () -> Promise<T>): Promise<T> =
		Promise<T> { m ->
			val promiseProxy = PromiseProxy(m)
			// Use resolve/rejection handler over `must` so that errors don't propagate as unhandled
			queuedPromises.offer { factory().also(promiseProxy::proxy).eventually(resolutionHandler, rejectionHandler) }
			doNext()
		}

	private fun doNext(): Promise<Unit> = synchronized(activePromiseSync) {
		queuedPromises
			.takeIf { ++activePromises <= rate }
			?.poll()
			?.invoke()
			?: run {
				--activePromises
				Unit.toPromise()
			}
	}

	private fun finishCurrentDoNext(): Promise<Unit> = synchronized(activePromiseSync) {
		activePromises = max(activePromises - 1, 0)

		return doNext()
	}
}
