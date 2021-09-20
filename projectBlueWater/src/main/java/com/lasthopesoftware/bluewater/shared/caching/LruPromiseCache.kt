package com.lasthopesoftware.bluewater.shared.caching

import androidx.collection.LruCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.util.concurrent.atomic.AtomicBoolean

class LruPromiseCache<Input : Any, Output>(maxValues: Int) : CachePromiseFunctions<Input, Output> {
	private val cachedPromises = LruCache<Input, PromiseBox<Output>>(maxValues)

	override fun getOrAdd(input: Input, factory: (Input) -> Promise<Output>): Promise<Output> {
		fun produceAndStoreValue(): Promise<Output> =
			synchronized(cachedPromises) {
				factory(input).also { p ->
					cachedPromises.put(input, PromiseBox(p))
				}
			}

		return cachedPromises[input]?.resolvedPromise
			?: synchronized(cachedPromises) {
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
