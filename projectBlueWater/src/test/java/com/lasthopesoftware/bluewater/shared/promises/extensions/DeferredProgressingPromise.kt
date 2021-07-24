package com.lasthopesoftware.bluewater.shared.promises.extensions

import kotlin.coroutines.cancellation.CancellationException

class DeferredProgressingPromise<Progress, Resolution> : ProgressingPromise<Progress, Resolution>(), Runnable {
	init {
		respondToCancellation(this)
	}

	fun sendProgressUpdate(progress: Progress) {
		reportProgress(progress)
	}

	fun sendProgressUpdates(vararg progresses: Progress) {
		for (progress in progresses) reportProgress(progress)
	}

	fun sendResolution(resolution: Resolution) {
		resolve(resolution)
	}

	fun sendRejection(rejection: Throwable) {
		reject(rejection)
	}

	override fun run() {
		reject(CancellationException())
	}
}
