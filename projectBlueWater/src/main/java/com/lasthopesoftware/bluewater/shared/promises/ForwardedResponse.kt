package com.lasthopesoftware.bluewater.shared.promises

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class ForwardedResponse<Resolution : Response, Response> private constructor() : ImmediateResponse<Resolution, Response> {
	override fun respond(resolution: Resolution): Response = resolution

	companion object {
		private val singlePassThrough by lazy { ForwardedResponse<Any, Any>() }

		@Suppress("UNCHECKED_CAST")
		@JvmStatic
		fun <Resolution : Response, Response> forward(): ForwardedResponse<Resolution, Response> =
			singlePassThrough as ForwardedResponse<Resolution, Response>

		fun <Resolution: Response, Response> Promise<Resolution>.forward(): Promise<Response> =
			this.then(Companion.forward())
	}
}
