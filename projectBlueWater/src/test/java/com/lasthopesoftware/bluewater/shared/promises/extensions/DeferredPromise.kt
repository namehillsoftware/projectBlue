package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.Promise

open class DeferredPromise<Resolution> : Promise<Resolution>, Runnable {
    private val resolution: Resolution?
    private val error: Throwable?

    constructor(resolution: Resolution) {
        this.resolution = resolution
        error = null
    }

    constructor(error: Throwable?) {
        resolution = null
        this.error = error
    }

	init {
	    respondToCancellation(this)
	}

    fun resolve() {
        if (resolution != null) resolve(resolution)
		else reject(error)
    }

	override fun run() {

	}
}
