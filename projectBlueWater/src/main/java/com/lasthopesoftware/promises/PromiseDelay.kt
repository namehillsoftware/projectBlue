package com.lasthopesoftware.promises

import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val delayScheduler by lazy { Executors.newScheduledThreadPool(0) }

class PromiseDelay<Response> private constructor(private val delay: Duration) : Promise<Response>(), Runnable, CancellationResponse {

	companion object {
		fun <Response> delay(delay: Duration): Promise<Response> = PromiseDelay(delay)
	}

	private val future = delayScheduler.schedule(this, delay.millis, TimeUnit.MILLISECONDS)

	init {
		awaitCancellation(this)
	}

	override fun run() {
		resolve(null)
	}

	override fun cancellationRequested() {
		future.cancel(false)
		reject(CancellationException("Delay $delay was cancelled."))
	}
}
