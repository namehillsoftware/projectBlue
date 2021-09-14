package com.lasthopesoftware.resources.executors

import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

internal object HttpThreadPoolExecutor {
	private val lazyExecutor by lazy {
		val maxDownloadThreadPoolSize = 4
		val downloadThreadPoolSize =
			maxDownloadThreadPoolSize.coerceAtMost(Runtime.getRuntime().availableProcessors())

		ForkJoinPool(downloadThreadPoolSize, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true)
	}

	val executor: ExecutorService get() = lazyExecutor
}
