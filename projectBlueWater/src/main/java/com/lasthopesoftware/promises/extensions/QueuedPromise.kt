package com.lasthopesoftware.promises.extensions

import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.queued.ExecutablePromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import java.util.concurrent.Executor
import kotlin.coroutines.cancellation.CancellationException


class QueuedPromise<Resolution>(private val task: CancellableMessageWriter<Resolution>, executor: Executor) :
	ExecutablePromise<Resolution>() {
	init {
		executor.execute(this)
	}

	override fun prepareMessage(cancellationSignal: CancellationSignal): Resolution {
		if (cancellationSignal.isCancelled) throw AbortedPromiseException()
		return task.prepareMessage(cancellationSignal)
	}

	class AbortedPromiseException : CancellationException("Message writing aborted.")
}
