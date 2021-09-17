package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

open class ProgressingPromiseProxy<Progress, Resolution> : ProgressingPromise<Progress, Resolution>() {
	private val cancellationProxy = CancellationProxy()
	private val proxyResolution = ImmediateResponse<Resolution, Unit> { resolve(it) }
	private val proxyRejection = ImmediateResponse<Throwable, Unit> { reject(it) }

	init {
		respondToCancellation(cancellationProxy)
	}

	protected fun proxy(source: ProgressingPromise<Progress, Resolution>): ProgressingPromiseProxy<Progress, Resolution> {
		cancellationProxy.doCancel(source)

		source.progress.then(UpdatesProxy(source))

		source.then(proxyResolution, proxyRejection)

		return this
	}

	protected fun proxySuccess(source: ProgressingPromise<Progress, Resolution>): ProgressingPromiseProxy<Progress, Resolution> {
		cancellationProxy.doCancel(source)

		source.progress.then(UpdatesProxy(source))

		source.then(proxyResolution)

		return this
	}

	protected fun doCancel(source: ProgressingPromise<Progress, Resolution>): ProgressingPromiseProxy<Progress, Resolution> {
		cancellationProxy.doCancel(source)
		return this
	}

	private inner class UpdatesProxy(private val source: ProgressingPromise<Progress, Resolution>) : ImmediateResponse<Progress, Unit>, (Progress) -> Unit {
		override fun respond(resolution: Progress) {
			if (resolution != null) reportProgress(resolution)
			source.updates(this)
		}

		override fun invoke(progress: Progress) {
			reportProgress(progress)
		}
	}
}
