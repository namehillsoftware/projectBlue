package com.lasthopesoftware.resources.executors

import com.google.common.util.concurrent.MoreExecutors
import org.joda.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool

object ThreadPools {

	// Maximum number to ensure no blocking
	val io by lazy { CachedManyThreadExecutor("io", Int.MAX_VALUE, Duration.standardMinutes(1)) }

	val compute by lazy {
		// Use a fork join pool for low latency while allowing for as many threads as needed to reduce risk of deadlocks
		val factory = ForkJoinPool.ForkJoinWorkerThreadFactory { pool ->
			ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool).apply {
				name = "compute-pool-$poolIndex"
			}
		}

		ForkJoinPool(Runtime.getRuntime().availableProcessors(), factory, null, true)
	}

	val exceptionsLogger: Executor by lazy { MoreExecutors.newSequentialExecutor(compute) }

	val database: Executor by lazy { MoreExecutors.newSequentialExecutor(io) }

	private val databaseThreadCacheSync = Any()

	private val databaseThreadCache = HashMap<Class<*>, Executor>()

	fun <T> databaseTableExecutor(cls: Class<T>) = databaseThreadCache[cls] ?: synchronized(databaseThreadCacheSync) {
		databaseThreadCache.getOrPut(cls, { MoreExecutors.newSequentialExecutor(io) })
	}

	inline fun <reified T> databaseTableExecutor() = databaseTableExecutor(T::class.java)
}
