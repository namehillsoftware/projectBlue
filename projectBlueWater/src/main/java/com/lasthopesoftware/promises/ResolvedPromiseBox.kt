package com.lasthopesoftware.promises

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse

class ResolvedPromiseBox<Resolution, ResolutionPromise : Promise<Resolution>>(val originalPromise: ResolutionPromise) : ImmediateResponse<Resolution, Unit>, PromisedResponse<Resolution, Resolution> {
	init { originalPromise.then(this) }

	override fun respond(resolution: Resolution) { resolvedPromise = originalPromise }

	fun forwardResolution(onError: PromisedResponse<Throwable, Resolution>): Promise<Resolution> = originalPromise.eventually(
		this,
		onError
	)

	@Volatile
	var resolvedPromise: ResolutionPromise? = null
		private set

	override fun promiseResponse(resolution: Resolution): Promise<Resolution> = originalPromise
}
