package com.lasthopesoftware.bluewater.shared.policies.retries

import com.namehillsoftware.handoff.promises.Promise

interface RetryPromises {
	fun <T> retryOnException(promiseFactory: (Throwable?) -> Promise<T>): Promise<T>
}
