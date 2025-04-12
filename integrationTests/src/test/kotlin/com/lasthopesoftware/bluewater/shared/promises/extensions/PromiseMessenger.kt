package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.Promise

class PromiseMessenger<Resolution> : Promise<Resolution>() {

    fun sendResolution(resolution: Resolution) {
        resolve(resolution)
    }

    fun sendRejection(error: Throwable) {
        reject(error)
    }
}
