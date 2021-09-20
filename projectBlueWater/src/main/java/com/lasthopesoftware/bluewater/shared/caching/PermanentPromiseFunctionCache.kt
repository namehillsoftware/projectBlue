package com.lasthopesoftware.bluewater.shared.caching

import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class PermanentPromiseFunctionCache<Input : Any, Output> : CachePromiseFunctions<Input, Output> {
	private val cachedSyncs = ConcurrentHashMap<Input, Any>()
	private val cachedPromises = ConcurrentHashMap<Input, PromiseBox<Output>>()

	override fun getOrAdd(input: Input, factory: (Input) -> Promise<Output>): Promise<Output> {
		fun produceAndStoreValue(): Promise<Output> =
			synchronized(cachedSyncs.getOrPut(input, ::Any)) {
				factory(input).also { p ->
					cachedPromises[input] = PromiseBox(p)
				}
			}

		return cachedPromises[input]?.resolvedPromise
			?: synchronized(cachedSyncs.getOrPut(input, ::Any)) {
				cachedPromises[input]?.resolvedPromise
					?: cachedPromises[input]?.promise?.eventually({ o -> o.toPromise() }, { produceAndStoreValue() })
					?: produceAndStoreValue()
			}
	}

	private class PromiseBox<Resolution>(val promise: Promise<Resolution>) : ImmediateResponse<Resolution, Unit> {
		private val isResolved = AtomicBoolean()

		init { promise.then(this) }

		override fun respond(resolution: Resolution) { isResolved.set(true) }

		val resolvedPromise: Promise<Resolution>?
			get() = promise.takeIf { isResolved.get() }
	}
}
