package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import org.joda.time.Duration
import java.util.concurrent.*

fun <Resolution> Promise<Resolution>.toExpiringFuture() = ExpiringFuturePromise(this)

class ExpiringFuturePromise<Resolution>(promise: Promise<Resolution>) : Future<Resolution?> {
	private val defaultCancellationDuration = Duration.standardSeconds(30)
	private val cancellationProxy = CancellationProxy()
	private val promise: Promise<Unit>
	private var resolution: Resolution? = null
	private var rejection: Throwable? = null
	private var isCompleted = false

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

	@Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
	override fun get(): Resolution? {
		val countDownLatch = CountDownLatch(1)
		promise.then { _ -> countDownLatch.countDown() }
		if (countDownLatch.await(defaultCancellationDuration.millis, TimeUnit.MILLISECONDS)) return getResolution()
		throw TimeoutException()
	}

	@Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
	override fun get(timeout: Long, unit: TimeUnit): Resolution? {
		val countDownLatch = CountDownLatch(1)
		promise.then { _ -> countDownLatch.countDown() }
		if (countDownLatch.await(timeout, unit)) return getResolution()
		throw TimeoutException()
	}

	@Throws(ExecutionException::class)
	private fun getResolution(): Resolution? {
		rejection?.also { throw ExecutionException(rejection) }
		return resolution
	}

	init {
		cancellationProxy.doCancel(promise)
		this.promise = promise
			.then({ r: Resolution ->
				resolution = r
				isCompleted = true
			}, { e: Throwable? ->
				rejection = e
				isCompleted = true
			})
	}
}
