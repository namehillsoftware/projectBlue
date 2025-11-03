package com.lasthopesoftware.promises

import com.namehillsoftware.handoff.cancellation.Cancellable
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

object PromiseMachines {
	fun <Resolution> repeat(function: () -> Promise<Resolution>, repetitions: Int) : Promise<Resolution> {
		var repeatCount = 0
		return loop { _, cancellable ->
			val promisedResult = function()
			if (++repeatCount >= repetitions) cancellable.cancel()
			promisedResult
		}
	}

	fun <Resolution> loop(function: (Resolution?, Cancellable) -> Promise<Resolution>) : Promise<Resolution> {
		return LoopMachine(function)
	}

	private class LoopMachine<Resolution>(private val function: (Resolution?, Cancellable) -> Promise<Resolution>) :
		ContinuableMachine<Resolution>(), ImmediateResponse<Resolution, Unit> {

		@Volatile
		private var currentValue: Resolution? = null

		init {
			next()
		}

		override fun next(): Promise<Resolution> =
			function(currentValue, this)
				.also {
					doCancel(it)
					it.then(this)
					proxyRejection(it)
				}

		override fun respond(resolution: Resolution) {
			currentValue = resolution
			if (!isCancelled) next()
			else resolve(resolution)
		}
	}

	abstract class ContinuableMachine<Resolution> : Promise.Proxy<Resolution>(), Continuable<Resolution> {
		fun start() {
			proxyResult(next())
		}
	}

	interface Continuable<Resolution> {
		fun next(): Promise<Resolution>
	}
}
