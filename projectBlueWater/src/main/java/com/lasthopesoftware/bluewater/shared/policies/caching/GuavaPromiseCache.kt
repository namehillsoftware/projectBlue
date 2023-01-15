package com.lasthopesoftware.bluewater.shared.policies.caching

import com.google.common.cache.Cache
import com.lasthopesoftware.bluewater.shared.promises.ResolvedPromiseBox
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

open class GuavaPromiseCache<Input : Any, Output>(
	private val cachedPromises: Cache<Input, ResolvedPromiseBox<Output, Promise<Output>>>
) : CachePromiseFunctions<Input, Output> {
	final override fun getOrAdd(input: Input, factory: (Input) -> Promise<Output>): Promise<Output> =
		cachedPromises.getIfPresent(input)?.resolvedPromise ?: buildNewIfNeeded(input, factory)

	private fun buildNewIfNeeded(input: Input, factory: (Input) -> Promise<Output>): Promise<Output> {
		var factoryBuiltPromise: Promise<Output>? = null
		val cachedPromiseBox = cachedPromises.get(input) { ResolvedPromiseBox(factory(input).also { factoryBuiltPromise = it }) }

		// If the factory built the promise that was returned by the cache, return it directly to the caller,
		// otherwise recursively call `getOrAdd` in the error condition, and otherwise pass through the result of the
		// cached promise.
		return factoryBuiltPromise.takeIf { it === cachedPromiseBox.originalPromise }
			?: cachedPromiseBox
				.originalPromise
				.eventually(
					{ it.toPromise() },
					{
						cachedPromises.asMap().remove(input, cachedPromiseBox)
						getOrAdd(input, factory)
					}
				)
	}
}
