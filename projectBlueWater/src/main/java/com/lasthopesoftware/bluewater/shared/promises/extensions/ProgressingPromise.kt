package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.vedsoft.futures.runnables.OneParameterAction
import java.util.concurrent.ConcurrentLinkedQueue

abstract class ProgressingPromise<Progress, Resolution> : Promise<Resolution> {
	private val updateListeners = ConcurrentLinkedQueue<OneParameterAction<Progress>>()

	constructor(messengerOperator: MessengerOperator<Resolution>?) : super(messengerOperator)
	protected constructor()

	abstract val progress: Progress

	protected fun reportProgress(progress: Progress) {
		for (action in updateListeners) action.runWith(progress)
	}

	fun updates(action: OneParameterAction<Progress>): ProgressingPromise<Progress, Resolution> {
		updateListeners.add(action)
		return this
	}
}
