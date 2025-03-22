package com.lasthopesoftware.bluewater.shared.exceptions

import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.exceptions.isOkHttpCanceled
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
		uncaughtException(ex)
	}

	override fun newUnhandledRejection(rejection: Throwable) {
		if (reportException(rejection)) {
			ThreadPools.exceptionsLogger.execute {
				logger.warn("An asynchronous exception has not yet been handled", rejection)
			}
		}
	}

	fun uncaughtException(ex: Throwable): Boolean {
		val isReportable = reportException(ex)
		if (isReportable) {
			logger.error("Uncaught Exception", ex)
		}

		return isReportable
	}

	private fun reportException(ex: Throwable): Boolean = when (ex) {
			is CancellationException -> {
				logDebug(UnhandledCancellationException, ex)
				false
			}
			is IOException -> {
				when {
					ex.isSocketClosedException() -> {
						logDebug(UnhandledSocketClosedException, ex)
						false
					}
					ex.isOkHttpCanceled() -> {
						logDebug(UnhandledCancellationException, ex)
						false
					}
					else -> true
				}
			}
			else -> true
		}

	private fun logDebug(message: String, ex: Throwable) {
		if (BuildConfig.DEBUG) {
			logger.debug(message, ex)
		}
	}
}
