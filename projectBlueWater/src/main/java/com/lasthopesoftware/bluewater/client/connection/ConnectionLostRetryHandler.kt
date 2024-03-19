package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.policies.retries.RecursivePromiseRetryHandler
import com.lasthopesoftware.policies.retries.RetryPromises
import com.lasthopesoftware.promises.PromiseDelay
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.io.IOException
import java.util.Locale

object ConnectionLostRetryHandler : RetryPromises {

	private val initialTimeToWait = Duration.standardSeconds(5)

	override fun <T> retryOnException(promiseFactory: (Throwable?) -> Promise<T>): Promise<T> {
		var attempts = 0
		var timeToWait = initialTimeToWait
		return RecursivePromiseRetryHandler.retryOnException { error ->
			attempts++

			when {
				attempts == 1 -> promiseFactory(error)
				attempts <= 3 && isErrorRetryable(error) -> {
					val originalTimeToWait = timeToWait
					timeToWait = timeToWait.multipliedBy(2)
					Promise.Proxy { cp ->
						if (cp.isCancelled) Promise(error)
						else PromiseDelay
							.delay<Any?>(originalTimeToWait)
							.also(cp::doCancel)
							.eventually { promiseFactory(error) }
					}
				}
				else -> Promise(error)
			}
		}
	}

	private fun isErrorRetryable(error: Throwable?): Boolean =
		error is IOException && (
			ConnectionLostExceptionFilter.isConnectionLostException(error) ||
				error.message?.lowercase(Locale.getDefault())?.contains("canceled") == true
			)
}
