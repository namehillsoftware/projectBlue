package com.lasthopesoftware.bluewater.shared.caching

import androidx.collection.LruCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class LruPromiseCache<Input : Any, Output>(maxValues: Int) : CachePromiseFunctions<Input, Output> {
	private val cachedPromises = LruCache<Input, Promise<Output>>(maxValues)

	override fun getOrAdd(input: Input, factory: (Input) -> Promise<Output>): Promise<Output> {
		fun produceAndStoreValue(): Promise<Output> =
			synchronized(cachedPromises) {
				cachedPromises[input] ?: factory(input).also { cachedPromises.put(input, it) }
			}

		return cachedPromises[input]
			?.eventually({ o -> o.toPromise() }, { produceAndStoreValue() })
			?: produceAndStoreValue()
	}
}
