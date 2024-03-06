package com.lasthopesoftware.bluewater.shared.policies.retries

import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

object RecursivePromiseRetryHandler : RetryPromises {
	override fun <T> retryOnException(promiseFactory: (Throwable?) -> Promise<T>): Promise<T> =
		promiseFactory(null)
			.eventually(
				{
					it.toPromise()
				},
				{ e ->
					promiseFactory(e)
				})
}
