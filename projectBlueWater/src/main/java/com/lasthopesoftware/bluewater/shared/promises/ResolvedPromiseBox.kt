package com.lasthopesoftware.bluewater.shared.promises

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse

class ResolvedPromiseBox<Resolution, ResolutionPromise : Promise<Resolution>>(val originalPromise: ResolutionPromise) : ImmediateResponse<Resolution, Unit>, PromisedResponse<Resolution, Resolution> {
	@Volatile
	private var _resolvedPromise: ResolutionPromise? = null

	init { originalPromise.then(this) }

	override fun respond(resolution: Resolution) { _resolvedPromise = originalPromise }

	fun forwardResolution(onError: PromisedResponse<Throwable, Resolution>): Promise<Resolution> = originalPromise.eventually(
		this,
		onError
	)

	val resolvedPromise: ResolutionPromise?
		get() = _resolvedPromise

	override fun promiseResponse(resolution: Resolution): Promise<Resolution> = originalPromise
}
