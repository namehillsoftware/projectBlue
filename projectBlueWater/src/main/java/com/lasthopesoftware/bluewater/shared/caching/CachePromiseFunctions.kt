package com.lasthopesoftware.bluewater.shared.caching

import com.namehillsoftware.handoff.promises.Promise

interface CachePromiseFunctions<Input : Any, Output> {
	fun getOrAdd(input: Input, factory: (Input) -> Promise<Output>): Promise<Output>
}
