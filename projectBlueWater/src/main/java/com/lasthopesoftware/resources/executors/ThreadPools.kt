package com.lasthopesoftware.resources.executors

import org.joda.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Executors.defaultThreadFactory

object ThreadPools {

	// Maximum number to ensure no blocking
	val io by lazy { CachedManyThreadExecutor("io", Int.MAX_VALUE, Duration.standardMinutes(1)) }

	val compute: ExecutorService by lazy {
		// Fixed thread pool for fast dispatch
		Executors.newFixedThreadPool(
			Runtime.getRuntime().availableProcessors(),
			PrefixedThreadFactory("compute", defaultThreadFactory())
		)
	}

	val exceptionsLogger by lazy { CachedSingleThreadExecutor("exceptionsLogger") }

	val database by lazy { CachedSingleThreadExecutor("database") }

	private val databaseThreadCacheSync = Any()

	private val databaseThreadCache = HashMap<Class<*>, CachedSingleThreadExecutor>()

	fun <T> databaseTableExecutor(cls: Class<T>) = databaseThreadCache[cls] ?: synchronized(databaseThreadCacheSync) {
		databaseThreadCache.getOrPut(cls, { CachedSingleThreadExecutor("database-${cls.canonicalName}") })
	}

	inline fun <reified T> databaseTableExecutor() = databaseTableExecutor(T::class.java)
}
