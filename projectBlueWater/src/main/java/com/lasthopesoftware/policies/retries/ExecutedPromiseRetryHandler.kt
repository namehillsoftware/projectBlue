package com.lasthopesoftware.policies.retries

import com.lasthopesoftware.promises.toFuture
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import java.util.concurrent.ExecutionException

object ExecutedPromiseRetryHandler : RetryPromises {
	override fun <T> retryOnException(promiseFactory: (Throwable?) -> Promise<T>): Promise<T> = QueuedPromise(
		fun(messenger: Messenger<T>) {
			val cancellationProxy = CancellationProxy()
			messenger.awaitCancellation(cancellationProxy)
			var error: Throwable? = null
			while (!cancellationProxy.isCancelled) {
				try {
					messenger.sendResolution(promiseFactory(error).also(cancellationProxy::doCancel).toFuture().get())
					return
				} catch (ee: ExecutionException) {
					val cause = ee.cause
					if (cause === error) break
					error = cause
				} catch (e: Throwable) {
					messenger.sendRejection(e)
					return
				}
			}

			messenger.sendRejection(error ?: RetriesCancelledException())
		}, ThreadPools.policy)
}
