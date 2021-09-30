package com.lasthopesoftware.bluewater.shared.promises

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.util.concurrent.atomic.AtomicReference

class ResolvedPromiseBox<Resolution, ResolutionPromise : Promise<Resolution>>(val originalPromise: ResolutionPromise) : ImmediateResponse<Resolution, Unit> {
	private val _resolvedPromise = AtomicReference<ResolutionPromise?>()

	init { originalPromise.then(this) }

	override fun respond(resolution: Resolution) { _resolvedPromise.set(originalPromise) }

	val resolvedPromise: ResolutionPromise?
		get() = _resolvedPromise.get()
}
