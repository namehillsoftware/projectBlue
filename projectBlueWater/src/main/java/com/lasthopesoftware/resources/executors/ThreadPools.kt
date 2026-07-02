package com.lasthopesoftware.resources.executors

import com.lasthopesoftware.bluewater.exceptions.UncaughtExceptionHandlerLogger
import com.lasthopesoftware.promises.extensions.preparePromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread
import java.util.concurrent.atomic.AtomicInteger

object ThreadPools {

	private class NamedThreadPoolFactory(private val poolName: String) : ForkJoinPool.ForkJoinWorkerThreadFactory {
		private val threadIndex = AtomicInteger(0)

		override fun newThread(pool: ForkJoinPool): ForkJoinWorkerThread =
			ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool).apply {
				name = "$poolName-pool-${threadIndex.getAndIncrement()}"
			}
	}

	val io by lazy {
		ForkJoinPool(
			Runtime.getRuntime().availableProcessors(),
			NamedThreadPoolFactory("io"),
			UncaughtExceptionHandlerLogger,
			true
		)
	}

	val compute by lazy {
		ForkJoinPool(
			Runtime.getRuntime().availableProcessors(),
			NamedThreadPoolFactory("compute"),
			UncaughtExceptionHandlerLogger,
			true
		)
	}

	val exceptionsLogger: Executor by lazy { CachedSingleThreadExecutor("exceptionsLogger") }

	fun <T> promiseTableMessage(messageWriter: CancellableMessageWriter<T>): Promise<T> =
		io.preparePromise(messageWriter)
}
