package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise

open class DeferredPromise<Resolution> : Promise<Resolution>, CancellationResponse {
    val resolution: Resolution?
    val error: Throwable?

	var isCancelled = false
		private set

    constructor(resolution: Resolution) {
        this.resolution = resolution
        error = null
    }

    constructor(error: Throwable?) {
        resolution = null
        this.error = error
    }

	init {
	    awaitCancellation(this)
	}

    fun resolve() {
        if (resolution != null) resolve(resolution)
		else reject(error)
    }

	override fun cancellationRequested() {
		isCancelled = true
	}
}
