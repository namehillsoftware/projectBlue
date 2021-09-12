package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.RejectionProxy
import com.namehillsoftware.handoff.promises.propagation.ResolutionProxy

class CancellableProxyPromise<Resolution>(cancellableMessenger: (CancellationProxy) -> Promise<Resolution>)
	: Promise<Resolution>({ m ->
		val cancellationProxy = CancellationProxy()
		m.cancellationRequested(cancellationProxy)
		cancellableMessenger(cancellationProxy).then(ResolutionProxy(m), RejectionProxy(m))
	})
