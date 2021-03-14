package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

open class ProgressingPromise<Progress, Resolution> : ProgressedPromise<Progress, Resolution> {
	private val updateListeners = ConcurrentLinkedQueue<(Progress) -> Unit>()
	private val atomicProgress: AtomicReference<Progress?> = AtomicReference()
	private var isResolved = false

	constructor(resolution: Resolution?) : super(resolution)
	constructor(messengerOperator: MessengerOperator<Resolution>?) : super(messengerOperator)
	protected constructor()

	override val progress: Promise<Progress>
		get() = Promise(atomicProgress.get())

	protected fun reportProgress(progress: Progress) {
		atomicProgress.lazySet(progress)
		for (action in updateListeners) action(progress)
	}

	fun updates(action: (Progress) -> Unit): ProgressingPromise<Progress, Resolution> {
		val currentProgress = atomicProgress.get()
		if (currentProgress != null)
			action(currentProgress)

		if (!isResolved) {
			updateListeners.add(action)
			must {
				isResolved = true
				updateListeners.remove(action)
			}
		}

		return this
	}

	protected fun proxy(source: ProgressingPromise<Progress, Resolution>): ProgressingPromise<Progress, Resolution> {
		source
			.updates { reportProgress(it) }
			.then({resolve(it)}, {reject(it)})

		return this
	}
}
