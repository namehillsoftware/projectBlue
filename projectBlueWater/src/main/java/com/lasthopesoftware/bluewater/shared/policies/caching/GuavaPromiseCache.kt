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
		var newPromise: Promise<Output>? = null

		// If a new promise is built, use its result, otherwise try again
		return cachedPromises.get(input) { ResolvedPromiseBox(factory(input).also { newPromise = it }) }.originalPromise.eventually(
			{ newPromise ?: it.toPromise() },
			{ newPromise ?: buildNewIfNeeded(input, factory) }
		)
	}
}
