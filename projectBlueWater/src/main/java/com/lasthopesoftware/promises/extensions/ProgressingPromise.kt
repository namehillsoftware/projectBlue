package com.lasthopesoftware.promises.extensions

import com.lasthopesoftware.bluewater.shared.update
import com.lasthopesoftware.bluewater.shared.updateConditionally
import com.lasthopesoftware.promises.ContinuableResult
import com.lasthopesoftware.promises.ContinuingResult
import com.lasthopesoftware.promises.HaltedResult
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.util.concurrent.atomic.AtomicReference

open class ProgressingPromise<Progress, Resolution> : ProgressedPromise<ContinuableResult<Progress>, Resolution> {
	private val currentAndNext = AtomicReference<Pair<ReportablePromisedUpdate<Progress>, ReportablePromisedUpdate<Progress>?>>(
		ReportablePromisedUpdate<Progress>().let {
			Pair(it, it)
		}
	)

	constructor(resolution: Resolution?) : super(resolution)

	constructor(rejection: Throwable) : super(rejection)

	constructor(messengerOperator: MessengerOperator<Resolution>?) : super(messengerOperator)
	protected constructor()

	init {
		must { _ ->
			currentAndNext.updateConditionally(
				{ (_, next) -> next != null },
				{ (current, next) ->
					next?.stop()
					Pair(current, null)
				}
			)
		}
	}

	override val progress: Promise<ContinuableResult<Progress>>
		get() = currentAndNext.get().first

	protected fun reportProgress(progress: Progress) {
		currentAndNext.update { pair -> pair.second?.let { Pair(it, it.pushForward(progress)) } ?: pair }
	}

	private inner class ReportablePromisedUpdate<Progress> : Promise<ContinuableResult<Progress>>() {
		private val next by lazy { ReportablePromisedUpdate<Progress>() }

		fun pushForward(progress: Progress) = next.also {
			resolve(ContinuingResult(progress, it))
		}

		fun stop() = resolve(HaltedResult.halted())
	}
}

inline fun <Progress, Resolution> ProgressedPromise<ContinuableResult<Progress>, Resolution>.onEach(crossinline action: (Progress) -> Unit): ProgressedPromise<ContinuableResult<Progress>, Resolution> {
	val progressResponse = object : ImmediateResponse<ContinuableResult<Progress>, Unit> {
		override fun respond(resolution: ContinuableResult<Progress>) {
			if (resolution is ContinuingResult) {
				action(resolution.current)
				resolution.next.then(this)
			}
		}
	}

	progress.then(progressResponse)

	return this
}

inline fun <Progress, Resolution> ProgressingPromise<Progress, Resolution>.onEachEventually(crossinline action: (Progress) -> Promise<Unit>): ProgressedPromise<ContinuableResult<Progress>, Resolution> {
	val progressResponse = object : PromisedResponse<ContinuableResult<Progress>, Unit> {
		override fun promiseResponse(resolution: ContinuableResult<Progress>?): Promise<Unit> =
			if (resolution is ContinuingResult) {
				action(resolution.current).eventually { resolution.next.eventually(this) }
			} else {
				Unit.toPromise()
			}
	}

	val parent = this
	return object : ProgressingPromiseProxy<Progress, Resolution>(), PromisedResponse<Unit, Resolution> {
		init {
			proxyProgress(parent)
			proxy(parent.progress.eventually(progressResponse).eventually(this))
		}

		override fun promiseResponse(resolution: Unit?): Promise<Resolution> = parent
	}
}

