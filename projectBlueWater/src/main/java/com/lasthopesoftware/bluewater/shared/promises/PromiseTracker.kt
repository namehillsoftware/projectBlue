package com.lasthopesoftware.bluewater.shared.promises

import com.lasthopesoftware.bluewater.shared.promises.extensions.guaranteedUnitResponse
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class PromiseTracker {
	private val activePromises = ConcurrentHashMap<Promise<*>, Promise<Unit>>()

	fun <T> track(promise: Promise<T>) {
		activePromises.getOrPut(promise) {
			val storedPromise = promise.guaranteedUnitResponse()
			activePromises[promise] = storedPromise
			storedPromise.must { activePromises.remove(promise) }
		}
	}

	fun promiseAllConcluded(): Promise<Unit> = Promise
		.whenAll(activePromises.values)
		.guaranteedUnitResponse()
		.must { activePromises.clear() }
}
