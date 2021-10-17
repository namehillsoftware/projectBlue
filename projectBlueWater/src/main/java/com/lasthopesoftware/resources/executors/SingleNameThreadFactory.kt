package com.lasthopesoftware.resources.executors

import java.util.concurrent.ThreadFactory

class SingleNameThreadFactory(private val name: String, private val threadFactory: ThreadFactory) : ThreadFactory {
	override fun newThread(r: Runnable?): Thread {
		val thread = threadFactory.newThread(r)
		thread.name = name
		return thread
	}
}
