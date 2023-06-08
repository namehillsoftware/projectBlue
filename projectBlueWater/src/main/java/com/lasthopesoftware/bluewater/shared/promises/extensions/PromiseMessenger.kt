package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise

class PromiseMessenger<Resolution> : Promise<Resolution>(), Messenger<Resolution> {
    override fun sendResolution(resolution: Resolution) {
        resolve(resolution)
    }

    override fun sendRejection(error: Throwable) {
        reject(error)
    }

    override fun cancellationRequested(response: Runnable) {
        respondToCancellation(response)
    }
}
