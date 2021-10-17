package com.lasthopesoftware.bluewater.shared.policies.caching

import com.google.common.cache.Cache
import com.lasthopesoftware.bluewater.shared.promises.ResolvedPromiseBox
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

open class GuavaPromiseCache<Input : Any, Output>(
	private val cachedPromises: Cache<Input, ResolvedPromiseBox<Output, Promise<Output>>>
) : CachePromiseFunctions<Input, Output> {
	final override fun getOrAdd(input: Input, factory: (Input) -> Promise<Output>): Promise<Output> =
		cachedPromises.getIfPresent(input)?.resolvedPromise
			?: cachedPromises.get(input, { ResolvedPromiseBox(factory(input)) }).originalPromise.eventually(
				{ o -> o.toPromise() },
				{ factory(input).also { cachedPromises.put(input, ResolvedPromiseBox(it)) } }
			)
}
