package com.lasthopesoftware.bluewater.shared.caching

import androidx.collection.LruCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class LruPromiseCache<Input : Any, Output>(maxValues: Int) : CachePromiseFunctions<Input, Output> {
	private val quickAccessCache = ConcurrentHashMap<Input, Promise<Output>>()
	private val cachedPromises = object : LruCache<Input, Promise<Output>>(maxValues) {
		override fun entryRemoved(evicted: Boolean, key: Input, oldValue: Promise<Output>, newValue: Promise<Output>?) {
			if (evicted || newValue == null) quickAccessCache.remove(key)

			if (size() <= quickAccessCache.size) return

			synchronized(this) {
				val snapshot = snapshot()
				quickAccessCache
					.filterKeys { !snapshot.containsKey(it) }
					.forEach { (k, v) -> quickAccessCache.remove(k, v) }
			}
		}
	}

	override fun getOrAdd(input: Input, factory: (Input) -> Promise<Output>): Promise<Output> {
		fun produceAndStoreValue(): Promise<Output> =
			synchronized(cachedPromises) {
				factory(input).also { p ->
					cachedPromises.put(input, p)
					p.then { quickAccessCache[input] = p }
				}
			}

		return quickAccessCache[input]
			?: synchronized(cachedPromises) { cachedPromises[input]?.eventually({ o -> o.toPromise() }, { produceAndStoreValue() }) }
			?: produceAndStoreValue()
	}
}
