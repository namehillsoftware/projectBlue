package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

open class ProgressingPromiseProxy<Progress, Resolution> : ProgressingPromise<Progress, Resolution>() {
	private val cancellationProxy = CancellationProxy()

	init {
		respondToCancellation(cancellationProxy)
	}

	protected fun proxy(source: ProgressingPromise<Progress, Resolution>): ProgressingPromiseProxy<Progress, Resolution> {
		with(source) {
			cancellationProxy.doCancel(this)

			updates(::reportProgress)
			then(::resolve, ::reject)
		}

		return this
	}
}
