package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

class PromiseMessenger<Resolution> : Promise<Resolution>(), Messenger<Resolution> {

	private val cancellationProxy = CancellationProxy()

	init {
		awaitCancellation(cancellationProxy)
	}

    override fun sendResolution(resolution: Resolution) {
        resolve(resolution)
    }

    override fun sendRejection(error: Throwable) {
        reject(error)
    }

	override fun promisedCancellation(): Promise<Void> = cancellationProxy.promisedCancellation()

	override fun isCancelled(): Boolean = cancellationProxy.isCancelled
}
