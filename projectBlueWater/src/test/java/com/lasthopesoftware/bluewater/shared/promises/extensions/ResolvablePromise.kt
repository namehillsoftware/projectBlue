package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise

open class ResolvablePromise<Resolution> : Promise<Resolution>(), CancellationResponse {
	var isResolved = false
		private set

    var isCancelled = false
		private set

	init {
	    awaitCancellation(this)
	}

    fun sendResolution(resolution: Resolution) {
		isResolved = true
        resolve(resolution)
    }

	fun sendRejection(rejection: Throwable) {
		isResolved = true
		reject(rejection)
	}

	override fun cancellationRequested() {
		isCancelled = true
	}
}
