package com.lasthopesoftware.bluewater.shared.exceptions

import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.rejections.UnhandledRejectionsReceiver
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.SocketException
import java.util.*
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLProtocolException

object LoggerUncaughtExceptionHandler : Thread.UncaughtExceptionHandler, UnhandledRejectionsReceiver {

	private val logger by lazy { LoggerFactory.getLogger(LoggerUncaughtExceptionHandler::class.java) }

	override fun uncaughtException(thread: Thread, ex: Throwable) {
		ThreadPools.exceptionsLogger.execute { logger.error("Uncaught Exception", ex) }
	}

	override fun newUnhandledRejection(rejection: Throwable) {
		fun handleCancellationException(exception: Throwable, matcher: (Throwable) -> Boolean) {
			if (logger.isDebugEnabled && matcher(exception)) {
				ThreadPools.exceptionsLogger.execute {
					logger.debug(
						"An asynchronous cancellation exception has not yet been handled",
						rejection
					)
				}
			}
		}

		ThreadPools.exceptionsLogger.execute {
			when (rejection) {
				is CancellationException -> handleCancellationException(rejection) { true }
				is SocketException -> handleCancellationException(rejection) { r ->
					val message = r.message
					message == null || !message.lowercase(Locale.getDefault()).contains("socket closed")
				}
				is IOException -> handleCancellationException(rejection) { r ->
					val message = r.message
					message == null || !message.lowercase(Locale.getDefault()).contains("canceled")
				}
				is SSLProtocolException -> handleCancellationException(rejection) { r ->
					val message = r.message
					message == null || !message.lowercase(Locale.getDefault()).contains("ssl handshake aborted")
				}
				else ->	logger.warn("An asynchronous exception has not yet been handled", rejection)
			}
		}
	}
}
