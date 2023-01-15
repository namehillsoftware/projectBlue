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
		return cachedPromises.get(input) { ResolvedPromiseBox(factory(input).also { factoryBuiltPromise = it }) }
			.originalPromise
			.eventually(
				// If a new promise was built, use its result, otherwise try again or pass through the previous factory
				// built result
				{ factoryBuiltPromise ?: it.toPromise() },
				{ e ->
					val cachedPromiseBox = cachedPromises.getIfPresent(input)
					when {
						// If an error occurs and the promise is the most recently built, then just propagate the error
						factoryBuiltPromise != null && cachedPromiseBox?.originalPromise == factoryBuiltPromise -> Promise(e)
						// If the cached promise resolved, propagate the result
						cachedPromiseBox?.resolvedPromise != null -> cachedPromiseBox.originalPromise
						// Otherwise, clear out the old entry and go back through the process (this could invalidate an in-progress promise)
						else -> {
							cachedPromises.asMap().remove(input, cachedPromiseBox)
							getOrAdd(input, factory)
						}
					}
				}
			)
	}
}
