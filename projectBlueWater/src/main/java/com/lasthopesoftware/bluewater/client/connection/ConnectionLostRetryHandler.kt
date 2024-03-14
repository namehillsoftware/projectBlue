package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.shared.policies.retries.RecursivePromiseRetryHandler
import com.lasthopesoftware.bluewater.shared.policies.retries.RetryPromises
import com.lasthopesoftware.promises.PromiseDelay
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

object ConnectionLostRetryHandler : RetryPromises {

	private val initialTimeToWait = Duration.standardSeconds(5)

	override fun <T> retryOnException(promiseFactory: (Throwable?) -> Promise<T>): Promise<T> {
		var attempts = 0
		var timeToWait = initialTimeToWait
		return RecursivePromiseRetryHandler.retryOnException { error ->
			attempts++

			when {
				attempts == 1 -> promiseFactory(error)
				attempts <= 3 && ConnectionLostExceptionFilter.isConnectionLostException(error) -> {
					val originalTimeToWait = timeToWait
					timeToWait = timeToWait.multipliedBy(2)
					Promise.Proxy { cp ->
						PromiseDelay
							.delay<Any?>(originalTimeToWait)
							.also(cp::doCancel)
							.eventually { promiseFactory(error) }
					}
				}
				else -> Promise(error)
			}
		}
	}
}
