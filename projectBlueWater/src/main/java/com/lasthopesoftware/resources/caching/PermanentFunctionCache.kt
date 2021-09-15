package com.lasthopesoftware.resources.caching

import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class PermanentFunctionCache<Input : Any, Output> : CacheFunctions<Input, Output> {
	private val cachedSyncs = ConcurrentHashMap<Input, Any>()
	private val cachedPromises = HashMap<Input, Promise<Output>>()
	private val cachedValues = HashMap<Input, Output>()

	override fun getOrAdd(input: Input, addFunc: (Input) -> Promise<Output>): Promise<Output> =
		cachedValues[input]?.toPromise() ?: synchronized(cachedSyncs.getOrPut(input, ::Any)) {
			cachedValues[input]?.toPromise()
				?: cachedPromises[input]?.eventually({ o -> o.toPromise() }, { addFunc(input).then { o -> o.also { cachedValues[input] = o } } })?.also { cachedPromises[input] = it }
				?: addFunc(input).then { o -> o.also { cachedValues[input] = it } }.also { cachedPromises[input] = it }
		}
}
