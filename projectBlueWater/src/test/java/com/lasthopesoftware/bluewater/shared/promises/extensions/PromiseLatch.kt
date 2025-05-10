package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.lasthopesoftware.bluewater.shared.update
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.util.concurrent.atomic.AtomicReference

class PromiseLatch : PromisingLatch, AutoCloseable {
	private val latch = AtomicReference(DeferredPromise(Unit))

	override fun open(): AutoCloseable {
		latch.get().resolve()
		return this
	}

	override fun close() {
		latch.update {
			it.resolve()
			DeferredPromise(Unit)
		}
	}

	override fun <Response> then(response: ImmediateResponse<Unit, Response>): Promise<Response> = latch.get().then(response)

	override fun <Response> eventually(response: PromisedResponse<Unit, Response>): Promise<Response> =
		latch.get().eventually(response)
}
