package com.lasthopesoftware.bluewater.shared.promises.extensions

class DeferredProgressingPromise<Progress, Resolution> : ProgressingPromise<Progress, Resolution>() {
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
}
