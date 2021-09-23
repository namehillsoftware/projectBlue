package com.lasthopesoftware.resources.executors

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class PrefixedThreadFactory(namePrefix: String, private val threadFactory: ThreadFactory) : ThreadFactory {

	private val namePrefix = "$namePrefix-"

	private val threadCounter = AtomicInteger(1)

	override fun newThread(r: Runnable?): Thread {
		val threadName = namePrefix + threadCounter.getAndIncrement()
		val thread = threadFactory.newThread(DecrementWrapper(r, threadCounter))
		thread.name = threadName
		return thread
	}

	private class DecrementWrapper(private val r: Runnable?, private val threadCounter: AtomicInteger): Runnable {
		override fun run() {
			try {
				r?.run()
			} finally {
				threadCounter.getAndDecrement()
			}
		}
	}
}
