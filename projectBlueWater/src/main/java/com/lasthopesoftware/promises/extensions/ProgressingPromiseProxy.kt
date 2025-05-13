package com.lasthopesoftware.promises.extensions

import com.lasthopesoftware.promises.ContinuableResult
import com.lasthopesoftware.promises.ContinuingResult
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

open class ProgressingPromiseProxy<Progress, Resolution> protected constructor() : ProgressingPromise<Progress, Resolution>() {
	private val cancellationProxy = CancellationProxy()
	private val resolutionProxy = Proxy.ResolutionProxy(this)
	private val rejectionProxy = Proxy.RejectionProxy(this)
	private val progressProxy = object : ImmediateResponse<ContinuableResult<Progress>, Unit> {
		override fun respond(resolution: ContinuableResult<Progress>) {
			if (resolution is ContinuingResult) {
				reportProgress(resolution.current)
				resolution.next.then(this)
			}
		}
	}

	init {
		awaitCancellation(cancellationProxy)
	}

	constructor(source: ProgressingPromise<Progress, Resolution>) : this() {
		proxy(source)
	}

	protected fun proxy(source: ProgressingPromise<Progress, Resolution>) {
		doCancel(source)

		proxyProgress(source)

		source.then(resolutionProxy, rejectionProxy)
	}

	protected fun proxy(source: Promise<Resolution>) {
		doCancel(source)

		source.then(resolutionProxy, rejectionProxy)
	}

	protected fun proxySuccess(source: ProgressingPromise<Progress, Resolution>) {
		source.then(resolutionProxy)
	}

	protected fun proxyRejection(source: ProgressingPromise<Progress, Resolution>) {
		source.excuse(rejectionProxy)
	}

	protected fun doCancel(source: Promise<*>) {
		cancellationProxy.doCancel(source)
	}

	protected fun proxyProgress(source: ProgressingPromise<Progress, *>) {
		source.progress.then(progressProxy)
	}
}
