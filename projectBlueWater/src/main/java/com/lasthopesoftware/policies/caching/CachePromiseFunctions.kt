package com.lasthopesoftware.policies.caching

import com.namehillsoftware.handoff.promises.Promise

interface CachePromiseFunctions<Input : Any, Output> {
	fun getOrAdd(input: Input, factory: (Input) -> Promise<Output>): Promise<Output>
	fun overrideCachedValue(input: Input, output: Output)
}
