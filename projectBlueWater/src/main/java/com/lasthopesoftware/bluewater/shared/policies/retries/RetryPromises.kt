package com.lasthopesoftware.bluewater.shared.policies.retries

import com.namehillsoftware.handoff.promises.Promise

interface RetryPromises {
	fun <T> retryOnException(retryDecider: (Throwable) -> Promise<Boolean>, promiseFactory: () -> Promise<T>): Promise<T>
}
