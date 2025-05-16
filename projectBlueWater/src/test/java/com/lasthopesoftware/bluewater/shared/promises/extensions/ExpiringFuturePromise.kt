package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.lasthopesoftware.promises.toFuture
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

private val defaultCancellationDuration = Duration.standardSeconds(30)

fun <Resolution> Promise<Resolution>.toExpiringFuture() = ExpiringFuturePromise(this)

class ExpiringFuturePromise<Resolution>(promise: Promise<Resolution>) : Future<Resolution?> {
	private val future = promise.toFuture()

	override fun cancel(mayInterruptIfRunning: Boolean): Boolean = future.cancel(mayInterruptIfRunning)

	override fun isCancelled(): Boolean = future.isCancelled

	override fun isDone(): Boolean = future.isDone

	override fun get(): Resolution? = get(defaultCancellationDuration.millis, TimeUnit.MILLISECONDS)

	override fun get(timeout: Long, unit: TimeUnit) = future.get(timeout, unit)
}
