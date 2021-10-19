package com.lasthopesoftware.bluewater.shared.policies.ratelimiting

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

class RateLimiter<T>(private val executor: Executor, rate: Int): RateLimitPromises<T>, Runnable {
	private val queueProcessorReference = AtomicReference<RateLimiter<T>>()
	private val semaphore = Semaphore(rate)
	private val queuedPromises = ConcurrentLinkedQueue<() -> Promise<Unit>>()
	private val semaphoreReleasingResolveHandler = ImmediateResponse<T, Unit> { semaphore.release() }
	private val semaphoreReleasingRejectionHandler = ImmediateResponse<Throwable, Unit> { semaphore.release() }

	override fun limit(factory: () -> Promise<T>): Promise<T> {
		return Promise<T> { m ->
			val promiseProxy = PromiseProxy(m)
			// Use resolve/rejection handler over `must` so that errors don't propagate as unhandled
			queuedPromises.offer { factory().also(promiseProxy::proxy).then(semaphoreReleasingResolveHandler, semaphoreReleasingRejectionHandler) }

			if (queueProcessorReference.compareAndSet(null, this)) executor.execute(this)
		}
	}

	override fun run() {
		try {
			var promiseFactory: (() -> Promise<Unit>)?
			while (queuedPromises.poll().also { promiseFactory = it } != null) {
				semaphore.acquire()
				promiseFactory?.invoke()
			}
		} finally {
			queueProcessorReference.set(null)
		}
	}
}
