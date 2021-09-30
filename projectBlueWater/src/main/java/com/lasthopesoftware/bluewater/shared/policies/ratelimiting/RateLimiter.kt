package com.lasthopesoftware.bluewater.shared.policies.ratelimiting

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

class RateLimiter<T>(private val executor: Executor, rate: Int): RateLimitPromises<T>, Runnable, ImmediateAction {
	private val queueProcessorReference = AtomicReference<RateLimiter<T>>()
	private val semaphore = Semaphore(rate)
	private val queuedPromises = ConcurrentLinkedQueue<() -> Promise<T>>()

	override fun limit(factory: () -> Promise<T>): Promise<T> {
		return Promise<T> { m ->
			val promiseProxy = PromiseProxy(m)
			queuedPromises.offer { factory().also(promiseProxy::proxy) }

			if (queueProcessorReference.compareAndSet(null, this)) executor.execute(this)
		}
	}

	override fun run() {
		try {
			var promiseFactory: (() -> Promise<T>)?
			while (queuedPromises.poll().also { promiseFactory = it } != null) {
				semaphore.acquire()
				promiseFactory?.invoke()?.must(this)
			}
		} finally {
			queueProcessorReference.set(null)
		}
	}

	override fun act() {
		semaphore.release()
	}
}
