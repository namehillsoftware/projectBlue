package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.shared.policies.retries.RecursivePromiseRetryHandler
import com.lasthopesoftware.bluewater.shared.policies.retries.RetryPromises
import com.lasthopesoftware.promises.PromiseDelay
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

object ConnectionLostRetryHandler : RetryPromises {
	override fun <T> retryOnException(promiseFactory: (Throwable?) -> Promise<T>): Promise<T> {
		var attempts = 0
		var timeToWait = Duration.standardSeconds(5)
		return RecursivePromiseRetryHandler.retryOnException { error ->
			attempts++

			when {
				attempts == 1 -> promiseFactory(error)
				attempts <= 3 && ConnectionLostExceptionFilter.isConnectionLostException(error) -> {
					val originalTimeToWait = timeToWait
					timeToWait = timeToWait.multipliedBy(2)
					PromiseDelay.delay<Any?>(originalTimeToWait).eventually { promiseFactory(error) }
				}
				else -> Promise(error)
			}
		}
	}
}
