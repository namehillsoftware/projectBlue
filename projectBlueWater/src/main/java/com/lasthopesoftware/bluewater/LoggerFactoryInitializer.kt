package com.lasthopesoftware.bluewater

import android.content.Context
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.startup.Initializer
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.util.StatusPrinter
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies
import com.lasthopesoftware.bluewater.exceptions.UncaughtExceptionHandlerLogger
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsUpdated
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.compilation.DebugFlag
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.ILoggerFactory
import org.slf4j.LoggerFactory
import java.io.File

class LoggerFactoryInitializer : Initializer<ILoggerFactory> {
	companion object {
		private val logger by lazyLogger<LoggerFactoryInitializer>()
	}

	@Volatile
	private var isLoggingToFile = true

	override fun create(context: Context): ILoggerFactory {
		Promise.Rejections.toggleStackTraceFiltering(true)

		val loggerFactory = initializeLogging(context)

		Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandlerLogger)
		Promise.Rejections.setUnhandledRejectionsReceiver(UncaughtExceptionHandlerLogger)

		with (context.applicationDependencies) {
			context
				.applicationDependencies
				.applicationSettings
				.promiseApplicationSettings()
				.then { settings ->
					reinitializeLoggingIfNecessary(context, settings)
				}

			registerForApplicationMessages.registerReceiver { _: ApplicationSettingsUpdated ->
				applicationSettings
					.promiseApplicationSettings()
					.then { settings ->
						reinitializeLoggingIfNecessary(context, settings)
					}
			}
		}

		return loggerFactory
	}

	override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

	private fun reinitializeLoggingIfNecessary(context: Context, applicationSettings: ApplicationSettings) {
		if (applicationSettings.isLoggingToFile != isLoggingToFile) {
			isLoggingToFile = applicationSettings.isLoggingToFile
			initializeLogging(context)

			logger.info("File logging {}.", if (isLoggingToFile) "enabled" else "disabled")
		}
	}

	private fun initializeLogging(context: Context): ILoggerFactory {
		val loggerFactory = LoggerFactory.getILoggerFactory()
		val lc = loggerFactory as? LoggerContext ?: return loggerFactory
		lc.reset()

		// setup LogcatAppender
		val logcatEncoder = PatternLayoutEncoder()
		logcatEncoder.context = lc
		logcatEncoder.pattern = "[%thread] %msg%n"
		logcatEncoder.start()
		val logcatAppender = LogcatAppender()
		logcatAppender.context = lc
		logcatAppender.encoder = logcatEncoder
		logcatAppender.start()
		// add the newly created appenders to the root logger;
		// qualify Logger to disambiguate from org.slf4j.Logger
		val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN
		rootLogger.addAppender(logcatAppender)
		if (isLoggingToFile && Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() && context.getExternalFilesDir(null) != null) {
			val asyncAppender = AsyncAppender()
			asyncAppender.context = lc
			asyncAppender.name = "ASYNC"
			val externalFilesDir = context.getExternalFilesDir(null)
			if (externalFilesDir != null) {
				val logDir = File(externalFilesDir.path + File.separator + "logs")
				if (!logDir.exists()) logDir.mkdirs()
				val rollingFileAppender = RollingFileAppender<ILoggingEvent>().apply {
					lazy = true
					isAppend = true
					this.context = lc
					file = File(logDir, "log.log").absolutePath

					rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>()
						.also { it.setParent(this) }
						.apply {
							fileNamePattern = File(logDir, "%d{yyyy-MM-dd}.log").absolutePath
							maxHistory = 5
							this.context = lc
							start()
						}

					encoder = PatternLayoutEncoder().apply {
						pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
						this.context = lc
						start()
					}

					start()
				}

				asyncAppender.addAppender(rollingFileAppender)
			}

			// UNCOMMENT TO TWEAK OPTIONAL SETTINGS
			// excluding caller data (used for stack traces) improves appender's performance
			asyncAppender.isIncludeCallerData = !DebugFlag.isDebugCompilation

			asyncAppender.start()
			rootLogger.addAppender(asyncAppender)
			StatusPrinter.print(lc)
		}

		if (DebugFlag.isDebugCompilation) {
			rootLogger.level = Level.DEBUG
			logger.info("DEBUG_MODE active")
			StrictMode.setThreadPolicy(
				StrictMode.ThreadPolicy.Builder()
					.detectDiskReads()
					.detectDiskWrites()
					.detectNetwork() // or .detectAll() for all detectable problems
					.penaltyLog()
					.build()
			)
			StrictMode.setVmPolicy(
				VmPolicy.Builder()
					.detectLeakedSqlLiteObjects()
					.penaltyLog()
					.build()
			)
		}

		return loggerFactory
	}
}
