package com.lasthopesoftware.bluewater.shared.promises

import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private val delayScheduler by lazy { Executors.newScheduledThreadPool(0) }

class PromiseDelay<Response> private constructor(delay: Duration) : Promise<Response>(), Runnable {

	companion object {
		fun <Response> delay(delay: Duration): Promise<Response> = PromiseDelay(delay)
	}

	init {
		val future = delayScheduler.schedule(this, delay.millis, TimeUnit.MILLISECONDS)
		respondToCancellation(FutureCancellation(future))
	}

	override fun run() {
		resolve(null)
	}

	private class FutureCancellation(private val future: ScheduledFuture<*>) : Runnable {
		override fun run() {
			future.cancel(false)
		}
	}
}
