package com.lasthopesoftware.promises

import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse

class ForwardedResponse<Resolution : Response, Response> private constructor() : ImmediateResponse<Resolution, Response>, PromisedResponse<Resolution, Response> {
	override fun respond(resolution: Resolution): Response = resolution

	override fun promiseResponse(resolution: Resolution): Promise<Response> = resolution.toPromise()

	companion object {
		private val singlePassThrough by lazy { ForwardedResponse<Any?, Any?>() }

		@Suppress("UNCHECKED_CAST")
		@JvmStatic
		fun <Resolution : Response, Response> forward(): ForwardedResponse<Resolution, Response> =
			singlePassThrough as ForwardedResponse<Resolution, Response>

		@Suppress("UNCHECKED_CAST")
		@JvmStatic
		fun <Resolution : Response, Response> promisedForward(): ForwardedResponse<Resolution, Response> =
			singlePassThrough as ForwardedResponse<Resolution, Response>

		fun <Resolution: Response, Response> Promise<Resolution>.forward(): Promise<Response> =
			this.then(Companion.forward())
	}
}
