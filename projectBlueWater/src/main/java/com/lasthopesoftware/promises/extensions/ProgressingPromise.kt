package com.lasthopesoftware.promises.extensions

import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

interface ContinuablePromise<Progress> {
	val current: Progress
	val next: Promise<ContinuablePromise<Progress>>
}

open class ProgressingPromise<Progress, Resolution> : ProgressedPromise<ContinuablePromise<Progress>, Resolution> {
	private val updateListeners = ConcurrentHashMap<(Progress) -> Unit, Unit>()
	private val currentAndNext = AtomicReference(ReportablePromisedUpdate<Progress>().let { Pair(it, it) })

	@Volatile
	private var isResolved = false

	constructor(resolution: Resolution?) : super(resolution)

	constructor(rejection: Throwable) : super(rejection)

	constructor(messengerOperator: MessengerOperator<Resolution>?) : super(messengerOperator)
	protected constructor()

	init {
		must { _ -> isResolved = true }
	}

	override val progress: Promise<ContinuablePromise<Progress>>
		get() = currentAndNext.get().first

	protected fun reportProgress(progress: Progress) {
		if (isResolved) return

		currentAndNext.updateAndGet { (_, next) ->
			Pair(next, next.pushForward(progress))
		}
	}

	private class ReportablePromisedUpdate<Progress> : Promise<ContinuablePromise<Progress>>() {
		fun pushForward(progress: Progress) = ReportablePromisedUpdate<Progress>().also {
			resolve(ResolvableContinuingProgress(progress, it))
		}
	}

	private class ResolvableContinuingProgress<Progress>(
		override val current: Progress,
		override val next: ReportablePromisedUpdate<Progress>,
	) : Promise<ContinuablePromise<Progress>>(), ContinuablePromise<Progress>
}

fun <Progress, Resolution> ProgressedPromise<ContinuablePromise<Progress>, Resolution>.onEach(action: (Progress) -> Unit): ProgressedPromise<ContinuablePromise<Progress>, Resolution> {
	val progressResponse = object : ImmediateResponse<ContinuablePromise<Progress>, Unit> {
		override fun respond(resolution: ContinuablePromise<Progress>) {
			action(resolution.current)
			resolution.next.then(this)
		}
	}
	progress.then(progressResponse)
	return this
}

fun <Progress, Resolution> ProgressedPromise<ContinuablePromise<Progress>, Resolution>.updates(action: (Progress) -> Unit): ProgressedPromise<ContinuablePromise<Progress>, Resolution> {
	var readyToReport = false

	val progressResponse = object : ImmediateResponse<ContinuablePromise<Progress>, Unit> {
		override fun respond(resolution: ContinuablePromise<Progress>) {
			// Block immediate reports, only want to report updates
			if (readyToReport)
				action(resolution.current)
			resolution.next.then(this)
		}
	}
	progress.then(progressResponse)
	readyToReport = true
	return this
}
