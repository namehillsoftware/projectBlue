package com.lasthopesoftware.policies.caching

import com.lasthopesoftware.promises.ResolvedPromiseBox
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class PermanentPromiseFunctionCache<Input : Any, Output> :
	CachePromiseFunctions<Input, Output> {
	private val cachedSyncs = ConcurrentHashMap<Input, Any>()
	private val cachedPromises = ConcurrentHashMap<Input, ResolvedPromiseBox<Output, Promise<Output>>>()

	override fun getOrAdd(input: Input, factory: (Input) -> Promise<Output>): Promise<Output> {
		fun produceAndStoreValue(): Promise<Output> =
			synchronized(cachedSyncs.getOrPut(input, ::Any)) {
				factory(input).also { p ->
					cachedPromises[input] = ResolvedPromiseBox(p)
				}
			}

		return cachedPromises[input]?.resolvedPromise
			?: synchronized(cachedSyncs.getOrPut(input, ::Any)) {
				cachedPromises[input]?.resolvedPromise
					?: cachedPromises[input]?.originalPromise?.eventually({ o -> o.toPromise() }, { produceAndStoreValue() })
					?: produceAndStoreValue()
			}
	}

	override fun overrideCachedValue(input: Input, output: Output) = synchronized(cachedSyncs.getOrPut(input, ::Any)) {
		cachedPromises[input] = ResolvedPromiseBox(output.toPromise())
	}
}
