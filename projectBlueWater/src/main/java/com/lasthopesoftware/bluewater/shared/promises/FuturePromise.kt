package com.lasthopesoftware.bluewater.shared.promises

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

fun <Resolution> Promise<Resolution>.toFuture(): Future<Resolution?> = FuturePromise(this)

// Get the result in less time than the Application Not Responding error from Android
fun <Resolution> Future<Resolution>.getSafely(): Resolution? = get(3, TimeUnit.SECONDS)

private class FuturePromise<Resolution>(promise: Promise<Resolution>) : Future<Resolution?> {
	private val cancellationProxy = CancellationProxy()
	private val promise: Promise<Unit>
	private val countDownLatch = CountDownLatch(1)

	private var resolution: Resolution? = null
	private var rejection: Throwable? = null
	private var isCompleted = false

	init {
		cancellationProxy.doCancel(promise)
		this.promise = promise
			.then({ r: Resolution ->
				resolution = r
				isCompleted = true
				countDownLatch.countDown()
			}, { e: Throwable? ->
				rejection = e
				isCompleted = true
				countDownLatch.countDown()
			})
	}

	override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
		if (isCompleted) return false
		cancellationProxy.run()
		return true
	}

	override fun isCancelled(): Boolean {
		return cancellationProxy.isCancelled
	}

	override fun isDone(): Boolean {
		return isCompleted
	}

	override fun get(): Resolution? {
		countDownLatch.await()
		return getResolution()
	}

	override fun get(timeout: Long, unit: TimeUnit): Resolution? {
		if (countDownLatch.await(timeout, unit)) return getResolution()
		throw TimeoutException("Timed out waiting for promise to resolve")
	}

	private fun getResolution(): Resolution? {
		rejection?.also { throw ExecutionException(it) }
		return resolution
	}
}
