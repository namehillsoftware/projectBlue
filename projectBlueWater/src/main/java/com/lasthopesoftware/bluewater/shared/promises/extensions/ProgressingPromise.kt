package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.vedsoft.futures.runnables.OneParameterAction
import java.util.concurrent.ConcurrentLinkedQueue

open class ProgressingPromise<Progress, Resolution> : Promise<Resolution> {
	private val updateListeners = ConcurrentLinkedQueue<OneParameterAction<Progress>>()
	private var p: Progress? = null

	constructor(resolution: Resolution) : super(resolution)
	constructor(messengerOperator: MessengerOperator<Resolution>?) : super(messengerOperator)
	protected constructor()

	var progress: Progress?
		get() {
			return p
		}
		private set(value) {
			p = value
		}

	protected fun reportProgress(progress: Progress) {
		this.progress = progress;
		for (action in updateListeners) action.runWith(progress)
	}

	fun updates(action: OneParameterAction<Progress>): ProgressingPromise<Progress, Resolution> {
		updateListeners.add(action)
		return this
	}
}
