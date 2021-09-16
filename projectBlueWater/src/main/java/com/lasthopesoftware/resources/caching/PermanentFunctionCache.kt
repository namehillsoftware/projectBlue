package com.lasthopesoftware.resources.caching

import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class PermanentFunctionCache<Input : Any, Output> : CacheFunctions<Input, Output> {
	private val cachedSyncs = ConcurrentHashMap<Input, Any>()
	private val cachedPromises = HashMap<Input, Promise<Output>>()
	private val cachedValues = HashMap<Input, Output>()

	override fun getOrAdd(input: Input, factory: (Input) -> Promise<Output>): Promise<Output> {
		fun Promise<Output>?.promiseValue(): Promise<Output> {
			fun produceAndStoreValue() = factory(input).then { o -> o.also { cachedValues[input] = o } }

			return this?.eventually({ o -> o.toPromise() }, { produceAndStoreValue() })
				?: produceAndStoreValue()
		}

		return cachedValues[input]?.toPromise() ?: synchronized(cachedSyncs.getOrPut(input, ::Any)) {
			cachedValues[input]?.toPromise()
				?: cachedPromises[input].promiseValue().also { cachedPromises[input] = it }
		}
	}
}
