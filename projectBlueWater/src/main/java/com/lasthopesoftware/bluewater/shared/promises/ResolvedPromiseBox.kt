package com.lasthopesoftware.bluewater.shared.promises

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class ResolvedPromiseBox<Resolution, ResolutionPromise : Promise<Resolution>>(val originalPromise: ResolutionPromise) : ImmediateResponse<Resolution, Unit> {
	@Volatile
	private var _resolvedPromise: ResolutionPromise? = null

	init { originalPromise.then(this) }

	override fun respond(resolution: Resolution) { _resolvedPromise = originalPromise }

	val resolvedPromise: ResolutionPromise?
		get() = _resolvedPromise
}
