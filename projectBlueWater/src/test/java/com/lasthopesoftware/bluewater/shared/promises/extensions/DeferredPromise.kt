package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.lasthopesoftware.bluewater.shared.NullBox
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise

open class DeferredPromise<Resolution> : Promise<Resolution>, CancellationResponse {
    val resolution: NullBox<Resolution>?
	lateinit var error: Throwable

	var isCancelled = false
		private set

    constructor(resolution: Resolution) {
        this.resolution = NullBox(resolution)
    }

    constructor(error: Throwable) {
        this.error = error
		resolution = null
    }

	init {
	    awaitCancellation(this)
	}

    fun resolve() {
        if (resolution != null) resolve(resolution.value)
		else reject(error)
    }

	override fun cancellationRequested() {
		isCancelled = true
	}
}
