package com.lasthopesoftware.resources.executors

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class NamedThreadFactory(namePrefix: String) : ThreadFactory {
	private val group by lazy {
		System.getSecurityManager()?.threadGroup ?: Thread.currentThread().threadGroup
	}

	private val namePrefix = "$namePrefix-"

	private val threadNumber = AtomicInteger(1)

	override fun newThread(r: Runnable?): Thread {
		val t = Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0)
		if (t.isDaemon) t.isDaemon = false
		if (t.priority != Thread.NORM_PRIORITY) t.priority = Thread.NORM_PRIORITY
		return t
	}
}
