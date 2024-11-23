package com.lasthopesoftware.promises.extensions

import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

open class ProgressingPromise<Progress, Resolution> : ProgressedPromise<Progress, Resolution> {
	private val updateListeners = ConcurrentHashMap<(Progress) -> Unit, Unit>()
	private val atomicProgress = AtomicReference<Progress>()
	private val isResolved = AtomicBoolean()

	constructor(resolution: Resolution?) : super(resolution)

	constructor(rejection: Throwable) : super(rejection)

	constructor(messengerOperator: MessengerOperator<Resolution>?) : super(messengerOperator)
	protected constructor()

	override val progress: Promise<Progress>
		get() = Promise(atomicProgress.get())

	protected fun reportProgress(progress: Progress) {
		if (isResolved.get()) return

		atomicProgress.lazySet(progress)
		for (action in updateListeners.keys) action(progress)
	}

	fun updates(action: (Progress) -> Unit): ProgressingPromise<Progress, Resolution> {
		if (isResolved.get()) return this

		updateListeners[action] = Unit
		must { _ ->
			isResolved.set(true)
			updateListeners.remove(action)
		}

		return this
	}
}
