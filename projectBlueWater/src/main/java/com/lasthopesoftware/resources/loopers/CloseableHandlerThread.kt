package com.lasthopesoftware.resources.loopers

import android.os.HandlerThread
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

abstract class CloseableHandlerThread(name: String?, priority: Int):
	HandlerThread(name, priority),
	PromisingCloseable
{
	override fun promiseClose(): Promise<Unit> =
		if (isInterrupted) quit().toPromise().unitResponse()
		else QueuedPromise({ quitSafely(); Unit }, ThreadPools.compute)
}
