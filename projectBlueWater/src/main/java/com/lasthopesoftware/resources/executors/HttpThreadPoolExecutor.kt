package com.lasthopesoftware.resources.executors

import android.os.Build
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

object HttpThreadPoolExecutor {
	private val lazyExecutor = object : AbstractSynchronousLazy<ExecutorService>() {
		override fun create(): ExecutorService {
			val maxDownloadThreadPoolSize = 4
			val downloadThreadPoolSize = Math.min(maxDownloadThreadPoolSize, Runtime.getRuntime().availableProcessors())

			return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				ForkJoinPool(downloadThreadPoolSize, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true)
			else
				CachedManyThreadExecutor(downloadThreadPoolSize, 5, TimeUnit.MINUTES)
		}
	}

	val executor: ExecutorService get() = lazyExecutor.`object`
}
