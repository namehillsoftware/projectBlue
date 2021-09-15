package com.lasthopesoftware.resources.caching

import com.namehillsoftware.handoff.promises.Promise

interface CacheFunctions<Input : Any, Output> {
	fun getOrAdd(input: Input, addFunc: (Input) -> Promise<Output>): Promise<Output>
}
