package com.lasthopesoftware.resources.executors

import java.util.concurrent.ThreadFactory

class SingleNameThreadFactory(private val name: String) : ThreadFactory {
	private val group by lazy {
		System.getSecurityManager()?.threadGroup ?: Thread.currentThread().threadGroup
	}

	override fun newThread(r: Runnable?): Thread {
		val t = Thread(group, r, name, 0)
		if (t.isDaemon) t.isDaemon = false
		if (t.priority != Thread.NORM_PRIORITY) t.priority = Thread.NORM_PRIORITY
		return t
	}
}
