package com.lasthopesoftware.bluewater.shared.policies.caching

import androidx.collection.LruCache
import com.lasthopesoftware.bluewater.shared.promises.ResolvedPromiseBox
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class LruPromiseCache<Input : Any, Output>(maxValues: Int) : CachePromiseFunctions<Input, Output> {
	private val cachedPromises = LruCache<Input, ResolvedPromiseBox<Output, Promise<Output>>>(maxValues)

	override fun getOrAdd(input: Input, factory: (Input) -> Promise<Output>): Promise<Output> {
		fun produceAndStoreValue(): Promise<Output> =
			synchronized(cachedPromises) {
				factory(input).also { p ->
					cachedPromises.put(input, ResolvedPromiseBox(p))
				}
			}

		return cachedPromises[input]?.resolvedPromise
			?: synchronized(cachedPromises) {
				cachedPromises[input]?.resolvedPromise
					?: cachedPromises[input]?.originalPromise?.eventually({ o -> o.toPromise() }, { produceAndStoreValue() })
					?: produceAndStoreValue()
			}
	}
}
