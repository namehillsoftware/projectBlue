package com.lasthopesoftware.bluewater.shared.exceptions

import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.rejections.UnhandledRejectionsReceiver
import org.slf4j.LoggerFactory

object LoggerUncaughtExceptionHandler : Thread.UncaughtExceptionHandler, UnhandledRejectionsReceiver {

	private val logger by lazy { LoggerFactory.getLogger(LoggerUncaughtExceptionHandler::class.java) }

	override fun uncaughtException(thread: Thread, ex: Throwable) {
		ThreadPools.exceptionsLogger.execute { logger.error("Uncaught Exception", ex) }
	}

	override fun newUnhandledRejection(rejection: Throwable) {
		ThreadPools.exceptionsLogger.execute {
			logger.warn("An asynchronous exception has not yet been handled", rejection)
		}
	}
}
