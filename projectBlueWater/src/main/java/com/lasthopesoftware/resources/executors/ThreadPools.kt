package com.lasthopesoftware.resources.executors

import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

object ThreadPools {

	private fun namedForkJoinWorkerThreadFactory(prefix: String) = ForkJoinPool.ForkJoinWorkerThreadFactory { pool ->
		ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool).apply {
			name = "$prefix-fj-pool-$poolIndex"
		}
	}

	val io by lazy {
		ForkJoinPool(
			32767,
			namedForkJoinWorkerThreadFactory("io"),
			null,
			false
		)
	}

	val compute by lazy {
		ForkJoinPool(
			Runtime.getRuntime().availableProcessors(),
			namedForkJoinWorkerThreadFactory("compute"),
			null,
			true
		)
	}

	val exceptionsLogger by lazy { CachedSingleThreadExecutor("exceptionsLogger") }

	val http by lazy {
		val maxDownloadThreadPoolSize = 4
		val downloadThreadPoolSize =
			maxDownloadThreadPoolSize.coerceAtMost(Runtime.getRuntime().availableProcessors())

		ForkJoinPool(
			downloadThreadPoolSize,
			namedForkJoinWorkerThreadFactory("http"),
			null,
			true
		)
	}

	val httpMedia by lazy {
		CachedManyThreadExecutor("httpMedia", 2, 1, TimeUnit.MINUTES)
		ForkJoinPool(
			2,
			namedForkJoinWorkerThreadFactory("httpMedia"),
			null,
			true
		)
	}

	val database by lazy { CachedSingleThreadExecutor("database") }

	private val databaseThreadCacheSync = Any()

	private val databaseThreadCache = HashMap<Class<*>, CachedSingleThreadExecutor>()

	fun <T> databaseTableExecutor(cls: Class<T>) = databaseThreadCache[cls] ?: synchronized(databaseThreadCacheSync) {
		databaseThreadCache.getOrPut(cls, { CachedSingleThreadExecutor("database-${cls.canonicalName}") })
	}

	inline fun <reified T> databaseTableExecutor() = databaseTableExecutor(T::class.java)
}
