package com.lasthopesoftware.resources.executors

import com.google.common.util.concurrent.MoreExecutors
import org.joda.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object ThreadPools {

	// Maximum number to ensure no blocking
	val io by lazy { CachedManyThreadExecutor("io", Int.MAX_VALUE, Duration.standardMinutes(1)) }

	val compute: Executor by lazy {
		// Use a fixed thread pool to ensure threads are always available to run computations
		Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), PrefixedThreadFactory("compute", Executors.defaultThreadFactory()))
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
