package com.lasthopesoftware.bluewater.shared.exceptions

import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.rejections.UnhandledRejectionsReceiver
import org.slf4j.LoggerFactory
import java.util.concurrent.CancellationException

private const val UnhandledCancellationException = "A CancellationException was unhandled"

object LoggerUncaughtExceptionHandler : Thread.UncaughtExceptionHandler, UnhandledRejectionsReceiver {

	private val logger by lazy { LoggerFactory.getLogger(LoggerUncaughtExceptionHandler::class.java) }

	override fun uncaughtException(thread: Thread, ex: Throwable) {
		if (ex is CancellationException) {
			if (logger.isDebugEnabled) {
				logger.debug(UnhandledCancellationException, ex)
			}
			return
		}

		ThreadPools.exceptionsLogger.execute { logger.error("Uncaught Exception", ex) }
	}

	override fun newUnhandledRejection(rejection: Throwable) {
		if (rejection is CancellationException) {
			if (logger.isDebugEnabled) {
				logger.debug(UnhandledCancellationException, rejection)
			}
			return
		}

		ThreadPools.exceptionsLogger.execute {
			logger.warn("An asynchronous exception has not yet been handled", rejection)
		}
	}
}
