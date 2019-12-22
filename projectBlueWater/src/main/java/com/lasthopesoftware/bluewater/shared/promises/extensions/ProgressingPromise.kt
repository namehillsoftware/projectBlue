package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.vedsoft.futures.runnables.OneParameterAction
import java.util.concurrent.ConcurrentLinkedQueue

open class ProgressingPromise<Progress, Resolution> : Promise<Resolution> {
	private val updateListeners = ConcurrentLinkedQueue<OneParameterAction<Progress>>()
	private var p: Progress? = null

	constructor(resolution: Resolution?) : super(resolution)
	constructor(messengerOperator: MessengerOperator<Resolution>?) : super(messengerOperator)
	protected constructor()

	open val progress: Progress?
		get() {
			return p
		}

	protected fun reportProgress(progress: Progress) {
		this.p = progress
		for (action in updateListeners) action.runWith(progress)
	}

	fun updates(action: OneParameterAction<Progress>): ProgressingPromise<Progress, Resolution> {
		val currentProgress = p;
		if (currentProgress != null)
			action.runWith(currentProgress)
		updateListeners.add(action)
		return this
	}
}
