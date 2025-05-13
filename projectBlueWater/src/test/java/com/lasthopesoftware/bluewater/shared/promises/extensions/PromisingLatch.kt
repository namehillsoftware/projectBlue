package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.lasthopesoftware.bluewater.shared.Gate
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse

interface PromisingLatch : Gate {
	fun <Response> then(response: ImmediateResponse<Unit, Response>): Promise<Response>

	fun <Response> eventually(response: PromisedResponse<Unit, Response>): Promise<Response>
}
