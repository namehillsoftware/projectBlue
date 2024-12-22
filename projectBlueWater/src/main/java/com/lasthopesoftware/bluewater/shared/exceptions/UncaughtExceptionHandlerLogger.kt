package com.lasthopesoftware.bluewater.shared.exceptions

import com.lasthopesoftware.exceptions.isSocketClosedException
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.rejections.UnhandledRejectionsReceiver
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.CancellationException

private const val UnhandledCancellationException = "A CancellationException was unhandled"
private const val UnhandledSocketClosedException = "A Socket closed exception was unhandled"

object UncaughtExceptionHandlerLogger : Thread.UncaughtExceptionHandler, UnhandledRejectionsReceiver {

	private val logger by lazy {
		LoggerFactory.getLogger(javaClass).apply {
			info("Uncaught exceptions logging to custom uncaught exception handler.")
		}
	}

	override fun uncaughtException(thread: Thread, ex: Throwable) {
		if (reportException(ex)) {
			logger.error("Uncaught Exception", ex)
		}
	}

	override fun newUnhandledRejection(rejection: Throwable) {
		if (reportException(rejection)) {
			ThreadPools.exceptionsLogger.execute {
				logger.warn("An asynchronous exception has not yet been handled", rejection)
			}
		}
	}

	private fun reportException(ex: Throwable): Boolean = when (ex) {
			is CancellationException -> {
				logger.debug(UnhandledCancellationException, ex)
				false
			}
			is IOException -> {
				ex.takeIf { it.isSocketClosedException() }
					?.let {
						logger.debug(UnhandledSocketClosedException, it)
						false
					}
					?: true
			}
			else -> true
		}
}
