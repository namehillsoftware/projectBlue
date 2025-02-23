package com.lasthopesoftware.policies.retries

import com.lasthopesoftware.promises.ForwardedResponse.Companion.promisedForward
import com.lasthopesoftware.promises.PromiseMachines
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse

object RecursivePromiseRetryHandler : RetryPromises {
	override fun <T> retryOnException(promiseFactory: (Throwable?) -> Promise<T>): Promise<T> = PromiseSuccess(promiseFactory)

	private class PromiseSuccess<T>(private val promiseFactory: (Throwable?) -> Promise<T>) :
		PromiseMachines.ContinuableMachine<T>(), PromisedResponse<Throwable, T> {

		@Volatile
		private var error: Throwable? = null

		init {
			start()
		}

		override fun next(): Promise<T> = if (!isCancelled) {
			promiseFactory(error)
				.also(::doCancel)
				.eventually(promisedForward(), this)
		} else {
			Promise(error ?: RetriesCancelledException())
		}

		override fun promiseResponse(rejection: Throwable?): Promise<T> =
			if (error === rejection || isCancelled) Promise(rejection ?: RetriesCancelledException())
			else {
				error = rejection
				next()
			}
	}
}
