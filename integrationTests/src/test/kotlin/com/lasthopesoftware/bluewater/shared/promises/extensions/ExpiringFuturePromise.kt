package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import org.joda.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

fun <Resolution> Promise<Resolution>.toExpiringFuture() = ExpiringFuturePromise(this)

class ExpiringFuturePromise<Resolution>(promise: Promise<Resolution>) : Future<Resolution?> {
	private val defaultCancellationDuration = Duration.standardSeconds(30)
	private val cancellationProxy = CancellationProxy()
	private val countDownLatch = CountDownLatch(1)

	private var resolution: Resolution? = null
	private var rejection: Throwable? = null
	private var isCompleted = false

	init {
		cancellationProxy.doCancel(promise)
		promise
			.then({ r ->
				resolution = r
				isCompleted = true
				countDownLatch.countDown()
			}, { e ->
				rejection = e
				isCompleted = true
				countDownLatch.countDown()
			})
	}

	override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
		if (isCompleted) return false
		cancellationProxy.cancellationRequested()
		return true
	}

	override fun isCancelled(): Boolean {
		return cancellationProxy.isCancelled
	}

	override fun isDone(): Boolean {
		return isCompleted
	}

	override fun get(): Resolution? = get(defaultCancellationDuration.millis, TimeUnit.MILLISECONDS)

	override fun get(timeout: Long, unit: TimeUnit): Resolution? {
		if (countDownLatch.await(timeout, unit)) return getResolution()
		throw TimeoutException("Timed out waiting $timeout $unit for promise to resolve")
	}

	private fun getResolution(): Resolution? {
		rejection?.also { throw ExecutionException(it) }
		return resolution
	}
}
