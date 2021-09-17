package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

open class ProgressingPromiseProxy<Progress, Resolution> : ProgressingPromise<Progress, Resolution>() {
	private val cancellationProxy = CancellationProxy()

	init {
		respondToCancellation(cancellationProxy)
	}

	protected fun proxy(source: ProgressingPromise<Progress, Resolution>): ProgressingPromiseProxy<Progress, Resolution> {
		cancellationProxy.doCancel(source)

		source.progress.then {
			if (it != null) reportProgress(it)
			source.updates(::reportProgress)
		}

		source.then(::resolve, ::reject)

		return this
	}

	protected fun proxySuccess(source: ProgressingPromise<Progress, Resolution>): ProgressingPromiseProxy<Progress, Resolution> {
		cancellationProxy.doCancel(source)

		source.progress.then {
			if (it != null) reportProgress(it)
			source.updates(::reportProgress)
		}

		source.then(::resolve)

		return this
	}
}
