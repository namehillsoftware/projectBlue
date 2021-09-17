package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

open class ProgressingPromiseProxy<Progress, Resolution> : ProgressingPromise<Progress, Resolution>() {
	private val cancellationProxy = CancellationProxy()
	private val proxyResolution = ImmediateResponse<Resolution, Unit> { resolve(it) }
	private val proxyRejection = ImmediateResponse<Throwable, Unit> { reject(it) }
	private val proxyUpdates = object : ImmediateResponse<Progress, Unit>, (Progress) -> Unit {
		override fun respond(resolution: Progress) {
			if (resolution != null) reportProgress(resolution)
		}

		override fun invoke(progress: Progress) {
			reportProgress(progress)
		}
	}

	init {
		respondToCancellation(cancellationProxy)
	}

	protected fun proxy(source: ProgressingPromise<Progress, Resolution>): ProgressingPromiseProxy<Progress, Resolution> {
		doCancel(source)

		proxyUpdates(source)

		source.then(proxyResolution, proxyRejection)

		return this
	}

	protected fun proxySuccess(source: ProgressingPromise<Progress, Resolution>): ProgressingPromiseProxy<Progress, Resolution> {
		doCancel(source)

		proxyUpdates(source)

		source.then(proxyResolution)

		return this
	}

	protected fun doCancel(source: ProgressingPromise<Progress, Resolution>): ProgressingPromiseProxy<Progress, Resolution> {
		cancellationProxy.doCancel(source)
		return this
	}

	protected fun proxyUpdates(source: ProgressingPromise<Progress, Resolution>): ProgressingPromiseProxy<Progress, Resolution> {
		source.progress.then(proxyUpdates)
		source.updates(proxyUpdates)
		return this
	}
}
