package com.lasthopesoftware.promises

import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.cancellation.Cancellable
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse

object PromiseMachines {
	fun <Resolution> repeat(function: () -> Promise<Resolution>, repetitions: Int) : Promise<Resolution> {
		var repeatCount = 0
		return loop { _, cancellable ->
			val promisedResult = function()
			if (++repeatCount >= repetitions) cancellable.cancel()
			promisedResult
		}
	}

	fun <Resolution> loop(function: (Continuable<Resolution>, Cancellable) -> Promise<Resolution>) : Promise<Resolution> {
		return LoopMachine(function)
	}

	private class LoopMachine<Resolution>(private val function: (Continuable<Resolution>, Cancellable) -> Promise<Resolution>) :
		ContinuableMachine<Resolution>(), PromisedResponse<Resolution, Resolution> {
		init {
			start()
		}

		override fun next(): Promise<Resolution> = function(this, this).eventually(this)

		override fun promiseResponse(resolution: Resolution): Promise<Resolution> =
			if (!isCancelled) next()
			else resolution.toPromise()
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
