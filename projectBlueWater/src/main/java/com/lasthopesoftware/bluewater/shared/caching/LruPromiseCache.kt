package com.lasthopesoftware.bluewater.shared.caching

import androidx.collection.LruCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class LruPromiseCache<Input : Any, Output>(maxValues: Int) : CachePromiseFunctions<Input, Output> {
	private val cachedSyncs = ConcurrentHashMap<Input, Any>()
	private val cachedPromises = HashMap<Input, Promise<Output>>()
	private val cachedValues = object : LruCache<Input, Box<Output>>(maxValues) {
		override fun entryRemoved(evicted: Boolean, key: Input, oldValue: Box<Output>, newValue: Box<Output>?) {
			synchronized(cachedSyncs.getOrPut(key, ::Any)) {
				cachedSyncs.remove(key)
				cachedPromises.remove(key)
			}
		}
	}

	override fun getOrAdd(input: Input, factory: (Input) -> Promise<Output>): Promise<Output> {
		fun Promise<Output>?.promiseValue(): Promise<Output> {
			fun produceAndStoreValue() =
				factory(input).then { o -> synchronized(cachedValues) { o.also { cachedValues.put(input, Box(o)) } } }

			return this
				?.eventually({ o -> o.toPromise() }, { produceAndStoreValue() })
				?: produceAndStoreValue()
		}

		return cachedValues[input]?.run { result.toPromise() } ?: synchronized(cachedSyncs.getOrPut(input, ::Any)) {
			cachedValues[input]?.run { result.toPromise() }
				?: cachedPromises[input].promiseValue().also { cachedPromises[input] = it }
		}
	}

	private data class Box<Output>(val result: Output)
}
