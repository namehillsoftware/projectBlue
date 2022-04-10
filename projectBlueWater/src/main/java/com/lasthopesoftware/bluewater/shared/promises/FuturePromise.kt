package com.lasthopesoftware.bluewater.shared.promises

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import java.util.concurrent.*

fun <Resolution> Promise<Resolution>.toFuture() = FuturePromise(this)

class FuturePromise<Resolution>(promise: Promise<Resolution>) : Future<Resolution?> {
	private val cancellationProxy = CancellationProxy()
	private val promise: Promise<Unit>
	private var resolution: Resolution? = null
	private var rejection: Throwable? = null
	private var isCompleted = false

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
		return getResolution()
	}

	override fun get(timeout: Long, unit: TimeUnit): Resolution? {
		val countDownLatch = CountDownLatch(1)
		promise.then {	countDownLatch.countDown() }
		if (countDownLatch.await(timeout, unit)) return getResolution()
		throw TimeoutException()
	}

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
