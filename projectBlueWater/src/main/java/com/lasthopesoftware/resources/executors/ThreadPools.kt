package com.lasthopesoftware.resources.executors

import com.lasthopesoftware.bluewater.exceptions.UncaughtExceptionHandlerLogger
import com.lasthopesoftware.promises.extensions.preparePromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool

object ThreadPools {

	private fun namedThreadPoolFactory(poolName: String) = ForkJoinPool.ForkJoinWorkerThreadFactory { pool ->
		ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool).apply {
			name = "$poolName-pool-$poolIndex"
		}
	}

	val io by lazy {
		ForkJoinPool(
			Runtime.getRuntime().availableProcessors(),
			namedThreadPoolFactory("io"),
			UncaughtExceptionHandlerLogger,
			true
		)
	}

	val compute: Executor by lazy {
		ForkJoinPool(
			Runtime.getRuntime().availableProcessors(),
			namedThreadPoolFactory("compute"),
			UncaughtExceptionHandlerLogger,
			true
		)
	}

	val exceptionsLogger: Executor by lazy { CachedSingleThreadExecutor("exceptionsLogger") }

	fun <T> promiseTableMessage(messageWriter: CancellableMessageWriter<T>): Promise<T> =
		io.preparePromise(messageWriter)
}
