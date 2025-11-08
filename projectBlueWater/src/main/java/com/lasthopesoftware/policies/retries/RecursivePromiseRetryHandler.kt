package com.lasthopesoftware.policies.retries

import com.lasthopesoftware.promises.PromiseMachines
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

object RecursivePromiseRetryHandler : RetryPromises {
	override fun <T> retryOnException(promiseFactory: (Throwable?) -> Promise<T>): Promise<T> = PromiseSuccess(promiseFactory)

	private class PromiseSuccess<T>(private val promiseFactory: (Throwable?) -> Promise<T>) :
		PromiseMachines.ContinuableMachine<T>(), ImmediateResponse<Throwable, Unit> {

		@Volatile
		private var error: Throwable? = null

		init {
			next()
		}

		override fun next(): Promise<T> = if (!isCancelled) {
			promiseFactory(error)
				.also {
					doCancel(it)
					proxyResolution(it)
					it.excuse(this)
				}
		} else {
			Promise(error ?: RetriesCancelledException())
		}

		override fun respond(rejection: Throwable?) {
			if (error === rejection || isCancelled) reject(rejection ?: RetriesCancelledException())
			else {
				error = rejection
				next()
			}
		}
	}
}
