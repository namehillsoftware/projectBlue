package com.lasthopesoftware.promises

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

fun <Resolution> Promise<Resolution>.toFuture(): Future<Resolution?> = FuturePromise(this)

// Get the result in less time than the Application Not Responding error from Android
fun <Resolution> Future<Resolution>.getSafely(): Resolution? = get(3, TimeUnit.SECONDS)

private class FuturePromise<Resolution>(promise: Promise<Resolution>) : Future<Resolution?> {
	private val cancellationProxy = CancellationProxy()
	private val countDownLatch = CountDownLatch(1)
	private val message = AtomicReference<Pair<Resolution?, Throwable?>?>()

	init {
		cancellationProxy.doCancel(promise)
		promise
			.then({ r ->
				if (message.compareAndSet(null, Pair(r, null)))
					countDownLatch.countDown()
			}, { e ->
				if (message.compareAndSet(null, Pair(null, e)))
					countDownLatch.countDown()
			})
	}

	override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
		if (isDone) return false
		cancellationProxy.cancellationRequested()
		return true
	}

	override fun isCancelled(): Boolean = cancellationProxy.isCancelled

	override fun isDone(): Boolean = message.get() != null

	override fun get(): Resolution? {
		countDownLatch.await()
		return getResolution()
	}

	override fun get(timeout: Long, unit: TimeUnit): Resolution? {
		if (countDownLatch.await(timeout, unit)) return getResolution()
		throw TimeoutException("Timed out waiting $timeout $unit for promise to resolve")
	}

	private fun getResolution(): Resolution? = message.get()?.let { (resolution, rejection) ->
		if (rejection != null)
			throw ExecutionException(rejection)
		resolution
	}
}
