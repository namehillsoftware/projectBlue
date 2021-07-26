package com.lasthopesoftware.bluewater.shared.promises.extensions

import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

open class ProgressingPromise<Progress, Resolution> : ProgressedPromise<Progress, Resolution>, Runnable {
	private val cancellationProxy = CancellationProxy()
	private val updateListeners = ConcurrentLinkedQueue<(Progress) -> Unit>()
	private val atomicProgress: AtomicReference<Progress?> = AtomicReference()
	private var isResolved = false

	constructor(resolution: Resolution?) : super(resolution)
	constructor(messengerOperator: MessengerOperator<Resolution>?) : super(messengerOperator)
	protected constructor()

	override fun run() {
		cancellationProxy.run()
	}

	override val progress: Promise<Progress>
		get() = Promise(atomicProgress.get())

	protected fun reportProgress(progress: Progress) {
		atomicProgress.lazySet(progress)
		for (action in updateListeners) action(progress)
	}

	fun updates(action: (Progress) -> Unit) {
		if (isResolved) return

		updateListeners.add(action)
		must {
			isResolved = true
			updateListeners.remove(action)
		}
	}
}
