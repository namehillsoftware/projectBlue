package com.lasthopesoftware.promises.extensions

import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.queued.ExecutablePromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import java.util.concurrent.Executor


class QueuedCancellablePromise<Resolution>(private val task: CancellableMessageWriter<Resolution>, executor: Executor) :
	ExecutablePromise<Resolution>()
{
	init {
		executor.execute(this)
	}

	override fun prepareMessage(cancellationSignal: CancellationSignal): Resolution =
		task.prepareMessage(cancellationSignal)
}
