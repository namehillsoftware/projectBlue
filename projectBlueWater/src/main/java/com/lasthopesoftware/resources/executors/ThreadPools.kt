package com.lasthopesoftware.resources.executors

import com.google.common.util.concurrent.MoreExecutors
import com.lasthopesoftware.bluewater.repository.Entity
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
		ForkJoinPool(Runtime.getRuntime().availableProcessors(), namedThreadPoolFactory("io"), null, true)
	}

	val compute: Executor by lazy {
		ForkJoinPool(Runtime.getRuntime().availableProcessors(), namedThreadPoolFactory("compute"), null, true)
	}

	val exceptionsLogger: Executor by lazy { CachedSingleThreadExecutor("exceptionsLogger") }

	val database: Executor by lazy { MoreExecutors.newSequentialExecutor(io) }

	private val databaseThreadCacheSync = Any()

	private val databaseThreadCache = HashMap<Class<*>, Executor>()

	fun <T : Entity> databaseTableExecutor(cls: Class<T>) = databaseThreadCache[cls] ?: synchronized(databaseThreadCacheSync) {
		databaseThreadCache.getOrPut(cls) { MoreExecutors.newSequentialExecutor(io) }
	}

	inline fun <T, reified Table : Entity> promiseTableMessage(messageWriter: CancellableMessageWriter<T>): Promise<T> =
		databaseTableExecutor(Table::class.java).preparePromise(messageWriter)
}
