package com.lasthopesoftware.bluewater.client.playback.service.exceptions

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.ExoPlaybackException
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration
import org.slf4j.LoggerFactory
import java.io.IOException

class PlaybackExceptionHandler(
	private val notificationsConfiguration: PlaybackNotificationsConfiguration) : HandlePlaybackExceptions {
	private var numberOfErrors = 0
	private var lastErrorTime: Long = 0

	override fun handle(error: Throwable) {
		when (error) {
			is PlaybackEngineInitializationException -> handlePlaybackEngineInitializationException(error)
			is PreparationException -> handlePreparationException(error)
			is IOException -> handleIoException(error)
			is ExoPlaybackException -> handleExoPlaybackException(error)
			is PlaybackException -> handlePlaybackException(error)
			else -> {
				logger.error("An unexpected error has occurred!", error)
				lazyNotificationController.getObject().removeAllNotifications()
			}
		}
	}

	private fun handlePlaybackEngineInitializationException(exception: PlaybackEngineInitializationException) {
		logger.error("There was an error initializing the playback engine", exception)
		lazyNotificationController.getObject().removeAllNotifications()
	}

	private fun handlePreparationException(preparationException: PreparationException) {
		logger.error("An error occurred during file preparation for file " + preparationException.positionedFile.serviceFile, preparationException)
		uncaughtExceptionHandler(preparationException.cause)
	}

	private fun handlePlaybackException(exception: PlaybackException) {
		when(val cause = exception.cause) {
			is ExoPlaybackException -> handleExoPlaybackException(cause)
			is IllegalStateException -> {
				logger.error("The player ended up in an illegal state - closing and restarting the player", exception)
				closeAndRestartPlaylistManager()
			}
			is IOException -> handleIoException(cause)
			else -> logger.error("An unexpected playback exception occurred", exception)
		}
	}

	private fun handleExoPlaybackException(exception: ExoPlaybackException) {
		logger.error("An ExoPlaybackException occurred")
		if (exception.cause != null) handle(exception.cause)
	}

	private fun handleIoException(exception: IOException) {
		logger.error("An IO exception occurred during playback", exception)
		handleDisconnection()
	}

	private fun handleDisconnection() {
		val currentErrorTime = System.currentTimeMillis()
		// Stop handling errors if more than the max errors has occurred
		if (++numberOfErrors > maxErrors) {
			// and the last error time is less than the error count reset duration
			if (currentErrorTime <= lastErrorTime + errorCountResetDuration) {
				logger.warn("Number of errors has not surpassed " + maxErrors + " in less than " + errorCountResetDuration + "ms. Closing and restarting playlist manager.")
				closeAndRestartPlaylistManager()
				return
			}

			// reset the error count if enough time has elapsed to reset the error count
			numberOfErrors = 1
		}
		lastErrorTime = currentErrorTime
		val builder = NotificationCompat.Builder(this, notificationsConfiguration.notificationChannel)
		builder.setOngoing(true)
		// Add intent for canceling waiting for connection to come back
		val intent = Intent(this, PlaybackService::class.java)
		intent.action = PlaybackService.Action.stopWaitingForConnection
		val pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
		builder.setContentIntent(pi)
		builder.setContentTitle(getText(R.string.lbl_waiting_for_connection))
		builder.setContentText(getText(R.string.lbl_click_to_cancel))
		lazyNotificationController.getObject().notifyBackground(PlaybackService.buildFullNotification(builder), PlaybackService.playingNotificationId)
		pollingSessionConnection = PollConnectionService.pollSessionConnection(this)
		pollingSessionConnection
			.then(connectionRegainedListener.getObject(), onPollingCancelledListener.getObject())
	}

	private companion object {
		private val logger = LoggerFactory.getLogger(PlaybackExceptionHandler::class.java)!!
		private const val maxErrors = 3
		private const val errorCountResetDuration = 1000
	}
}
