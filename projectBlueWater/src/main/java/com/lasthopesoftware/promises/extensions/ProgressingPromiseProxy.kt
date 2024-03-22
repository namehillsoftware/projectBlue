package com.lasthopesoftware.promises.extensions

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

open class ProgressingPromiseProxy<Progress, Resolution> protected constructor() : ProgressingPromise<Progress, Resolution>() {
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

	constructor(source: ProgressingPromise<Progress, Resolution>) : this() {
		proxy(source)
	}

	init {
		awaitCancellation(cancellationProxy)
	}

	protected fun proxy(source: ProgressingPromise<Progress, Resolution>) {
		doCancel(source)

		proxyUpdates(source)

		source.then(proxyResolution, proxyRejection)
	}

	protected fun proxy(source: Promise<Resolution>) {
		doCancel(source)

		source.then(proxyResolution, proxyRejection)
	}

	protected fun proxySuccess(source: ProgressingPromise<Progress, Resolution>) {
		source.then(proxyResolution)
	}

	protected fun proxyRejection(source: ProgressingPromise<Progress, Resolution>) {
		source.excuse(proxyRejection)
	}

	protected fun doCancel(source: Promise<*>) {
		cancellationProxy.doCancel(source)
	}

	protected fun proxyUpdates(source: ProgressingPromise<Progress, *>) {
		source.progress.then(proxyUpdates)
		source.updates(proxyUpdates)
	}
}
