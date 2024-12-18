package com.lasthopesoftware.promises.extensions

import com.lasthopesoftware.bluewater.shared.updateConditionally
import com.lasthopesoftware.bluewater.shared.updateConditionallyWithNext
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

open class ProgressingPromise<Progress, Resolution> : ProgressedPromise<ContinuablePromise<Progress>, Resolution> {
	private val updateListeners = ConcurrentHashMap<(Progress) -> Unit, Unit>()
	private val currentAndNext = AtomicReference<Pair<ReportablePromisedUpdate<Progress>, ReportablePromisedUpdate<Progress>?>>(
		ReportablePromisedUpdate<Progress>().let {
			Pair(it, it)
		}
	)

	@Volatile
	private var isResolved = false

	constructor(resolution: Resolution?) : super(resolution)

	constructor(rejection: Throwable) : super(rejection)

	constructor(messengerOperator: MessengerOperator<Resolution>?) : super(messengerOperator)
	protected constructor()

	init {
		must { _ ->
			isResolved = true
			currentAndNext.updateConditionally(
				{ (_, next) -> next != null },
				{ (current, _) -> Pair(current, null) }
			)
		}
	}

	override val progress: Promise<ContinuablePromise<Progress>>
		get() = currentAndNext.get().first

	protected fun reportProgress(progress: Progress) {
		if (isResolved) return

		currentAndNext.updateConditionallyWithNext(
			{ prev, next -> prev != next },
			{ pair -> pair.second?.let { Pair(it, it.pushForward(progress)) } ?: pair }
		)
	}

	private inner class ReportablePromisedUpdate<Progress> : Promise<ContinuablePromise<Progress>>() {
		private val next by lazy { ReportablePromisedUpdate<Progress>() }

		fun pushForward(progress: Progress) = (if (!isResolved) next else this).also {
			resolve(ResolvableContinuingProgress(progress, it))
		}
	}

	private inner class ResolvableContinuingProgress<Progress>(
		override val current: Progress,
		override val next: ReportablePromisedUpdate<Progress>?,
	) : ContinuablePromise<Progress>
}

inline fun <Progress, Resolution> ProgressedPromise<ContinuablePromise<Progress>, Resolution>.onEach(crossinline action: (Progress) -> Unit): ProgressedPromise<ContinuablePromise<Progress>, Resolution> {
	val progressResponse = object : ImmediateAction, ImmediateResponse<ContinuablePromise<Progress>, Unit> {
		@Volatile
		private var isResolved = false

		override fun act() {
			isResolved = true
		}

		override fun respond(resolution: ContinuablePromise<Progress>) {
			action(resolution.current)

			if (!isResolved)
				resolution.next?.then(this)
		}
	}

	must(progressResponse)
	progress.then(progressResponse)

	return this
}
