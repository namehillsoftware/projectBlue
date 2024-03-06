package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import kotlin.coroutines.cancellation.CancellationException

class DeferredProgressingPromise<Progress, Resolution> : ProgressingPromise<Progress, Resolution>(), CancellationResponse {
	init {
		awaitCancellation(this)
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

	override fun cancellationRequested() {
		reject(CancellationException())
	}
}
