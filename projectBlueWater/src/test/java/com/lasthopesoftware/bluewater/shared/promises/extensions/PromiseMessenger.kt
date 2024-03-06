package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

class PromiseMessenger<Resolution> : Promise<Resolution>() {

    fun sendResolution(resolution: Resolution) {
        resolve(resolution)
    }

    fun sendRejection(error: Throwable) {
        reject(error)
    }
}
