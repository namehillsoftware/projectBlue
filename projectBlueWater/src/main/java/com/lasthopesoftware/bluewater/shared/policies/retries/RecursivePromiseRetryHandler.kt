package com.lasthopesoftware.bluewater.shared.policies.retries

import com.lasthopesoftware.promises.ForwardedResponse.Companion.promisedForward
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse

object RecursivePromiseRetryHandler : RetryPromises {
	override fun <T> retryOnException(promiseFactory: (Throwable?) -> Promise<T>): Promise<T> {
		return AttemptPromise(promiseFactory)
	}

	private class AttemptPromise<T>(
		private val promiseFactory: (Throwable?) -> Promise<T>,
		error: Throwable? = null
	) : Promise.Proxy<T>(), PromisedResponse<Throwable, T> {
		init {
			proxy(
				promiseFactory(error)
					.also(cancellationProxy::doCancel)
					.eventually(promisedForward(), this)
			)
		}

		override fun promiseResponse(error: Throwable?): Promise<T> =
			if (cancellationProxy.isCancelled) Promise(error)
			else AttemptPromise(promiseFactory, error)
	}
}
