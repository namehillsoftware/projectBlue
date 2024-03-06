package com.lasthopesoftware.promises

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class NoopResponse<Resolution> private constructor() : ImmediateResponse<Resolution, Unit> {
	override fun respond(resolution: Resolution) {}

	companion object {
		private val singleNoOp by lazy { NoopResponse<Any?>() }

		@Suppress("UNCHECKED_CAST")
		@JvmStatic
		fun <Resolution> noOpResponse(): NoopResponse<Resolution> =
			singleNoOp as NoopResponse<Resolution>

		fun <Resolution> Promise<Resolution>.ignore(): Promise<Unit> =
			this.then(noOpResponse(), noOpResponse())
	}
}
