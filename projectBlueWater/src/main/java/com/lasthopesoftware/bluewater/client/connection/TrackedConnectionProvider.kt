package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.shared.promises.extensions.guaranteedUnitResponse
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap

class TrackedConnectionProvider(private val inner: IConnectionProvider) : IConnectionProvider by inner, PromisingCloseable {

	private val activeCalls = ConcurrentHashMap<Promise<Response>, Promise<Unit>>()

	override fun promiseResponse(vararg params: String): Promise<Response> {
		val innerCall = inner.promiseResponse(*params)

		activeCalls.getOrPut(innerCall) {
			val storedPromise = innerCall.guaranteedUnitResponse()
			activeCalls[innerCall] = storedPromise
			storedPromise.must { activeCalls.remove(innerCall) }
		}

		return innerCall
	}

	override fun promiseClose(): Promise<Unit> = Promise
		.whenAll(activeCalls.values)
		.guaranteedUnitResponse()
		.must { activeCalls.clear() }
}
