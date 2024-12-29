package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.exceptions.isOkHttpCanceled
import com.lasthopesoftware.policies.retries.RetryPromises
import com.lasthopesoftware.promises.PromiseDelay
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import org.joda.time.Duration
import java.io.IOException

class ConnectionLostRetryHandler(private val innerHandler: RetryPromises) : RetryPromises {

	override fun <T> retryOnException(promiseFactory: (Throwable?) -> Promise<T>): Promise<T> {
		return innerHandler.retryOnException(ConnectionLostRetryMachine(promiseFactory))
	}

	private class ConnectionLostRetryMachine<T>(private val promiseFactory: (Throwable?) -> Promise<T>) : (Throwable?) -> Promise<T> {

		companion object {
			private val initialTimeToWait = Duration.standardSeconds(5)

			private fun isErrorRetryable(error: Throwable?): Boolean =
				error is IOException && (
					ConnectionLostExceptionFilter.isConnectionLostException(error) || error.isOkHttpCanceled())
		}

		@Volatile
		private var attempts = 0

		@Volatile
		private var timeToWait = initialTimeToWait

		override fun invoke(error: Throwable?): Promise<T> {
			attempts++

			return when {
				attempts == 1 -> promiseFactory(error)
				attempts <= 3 && isErrorRetryable(error) -> {
					val originalTimeToWait = timeToWait
					timeToWait = timeToWait.multipliedBy(2)
					ProxiedRetry(originalTimeToWait, error, promiseFactory)
				}
				else -> Promise(error)
			}
		}
	}

	private class ProxiedRetry<T>(
		timeToWait: Duration,
		private val error: Throwable?,
		private val promiseFactory: (Throwable?) -> Promise<T>
	) : Promise.Proxy<T>(), PromisedResponse<Any?, T> {
		init {
			if (isCancelled) reject(error)
			else proxy(
				PromiseDelay
					.delay<Any?>(timeToWait)
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(resolution: Any?): Promise<T> = promiseFactory(error)
	}
}
