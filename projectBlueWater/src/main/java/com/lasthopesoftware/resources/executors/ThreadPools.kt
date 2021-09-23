package com.lasthopesoftware.resources.executors

import org.joda.time.Duration

object ThreadPools {

	val io by lazy { CachedManyThreadExecutor("io", Int.MAX_VALUE, Duration.standardMinutes(1)) }

	val compute by lazy { CachedManyThreadExecutor("compute", Runtime.getRuntime().availableProcessors(), Duration.standardMinutes(3)) }

	val exceptionsLogger by lazy { CachedSingleThreadExecutor("exceptionsLogger") }

	val database by lazy { CachedSingleThreadExecutor("database") }

	private val databaseThreadCacheSync = Any()

	private val databaseThreadCache = HashMap<Class<*>, CachedSingleThreadExecutor>()

	fun <T> databaseTableExecutor(cls: Class<T>) = databaseThreadCache[cls] ?: synchronized(databaseThreadCacheSync) {
		databaseThreadCache.getOrPut(cls, { CachedSingleThreadExecutor("database-${cls.canonicalName}") })
	}

	inline fun <reified T> databaseTableExecutor() = databaseTableExecutor(T::class.java)
}
