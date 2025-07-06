package com.lasthopesoftware.bluewater.client.playback.service

import androidx.annotation.OptIn
import androidx.media3.common.ParserException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlaybackException
import com.lasthopesoftware.bluewater.android.services.ControlService
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.polling.PollForLibraryConnections
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaybackState
import com.lasthopesoftware.bluewater.client.playback.engine.ChangePlaylistPosition
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaylistError
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException
import com.lasthopesoftware.bluewater.client.playback.errors.PlaybackResourceNotAvailableInTimeException
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException
import com.lasthopesoftware.bluewater.exceptions.AnnounceExceptions
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.resilience.TimedCountdownLatch
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import org.joda.time.Duration
import java.io.IOException
import java.net.ProtocolException
import java.util.concurrent.CancellationException

class PlaybackErrorHandler(
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val pollConnectionServiceProxy: PollForLibraryConnections,
	private val changePlaylistPosition: ChangePlaylistPosition,
	private val changePlaybackState: ChangePlaybackState,
	private val serviceControl: ControlService,
	private val resetPlaybackService: ResetPlaybackService,
	private val exceptionAnnouncer: AnnounceExceptions,
) : OnPlaylistError {
	companion object {
		private val logger by lazyLogger<PlaybackService>()

		private const val numberOfDisconnects = 3
		private val disconnectResetDuration = Duration.standardMinutes(1)
		private const val numberOfErrors = 5
		private val errorLatchResetDuration = Duration.standardSeconds(3)
	}

	private val errorLatch by lazy { TimedCountdownLatch(numberOfErrors, errorLatchResetDuration) }
	private val disconnectionLatch by lazy { TimedCountdownLatch(numberOfDisconnects, disconnectResetDuration) }
	private val connectionRegainedListener by lazy { ImmediateResponse<LiveServerConnection, Unit> { resetPlaybackService.resetPlaylistManager() } }
	private val onPollingCancelledListener by lazy {
		ImmediateResponse<Throwable?, Unit> { e ->
			if (e is CancellationException) {
				serviceControl.stop()
			}
		}
	}

	override fun onError(error: Throwable) {
		uncaughtExceptionHandler(error)
	}

	private fun uncaughtExceptionHandler(exception: Throwable?) {
		when (exception) {
			is CancellationException -> return
			is PlaybackEngineInitializationException -> handlePlaybackEngineInitializationException(exception)
			is PreparationException -> handlePreparationException(exception)
			is IOException -> handleIoException(exception)
			is androidx.media3.common.PlaybackException -> handleMedia3PlaybackException(exception)
			is PlaybackException -> handlePlaybackException(exception)
			is PlaybackResourceNotAvailableInTimeException -> handleTimeoutException(exception)
			else -> {
				logger.error("An unexpected error has occurred!", exception)
				if (exception != null)
					exceptionAnnouncer.announce(exception)
				serviceControl.stop()
			}
		}
	}

	private fun handleDisconnection(exception: IOException?) {
		if (disconnectionLatch.trigger()) {
			logger.error("Unable to re-connect after $numberOfDisconnects in less than $disconnectResetDuration, stopping the playback service.")

			exception?.also(exceptionAnnouncer::announce)

			serviceControl.stop()
			return
		}

		logger.warn("Number of disconnections has not surpassed $numberOfDisconnects in less than $disconnectResetDuration. Checking for disconnections.")

		selectedLibraryIdProvider
			.promiseSelectedLibraryId()
			.then { libraryId ->
				libraryId
					?.let(pollConnectionServiceProxy::pollConnection)
					?.then(connectionRegainedListener, onPollingCancelledListener)
			}
	}

	private fun handlePlaybackEngineInitializationException(exception: PlaybackEngineInitializationException) {
		logger.error("There was an error initializing the playback engine", exception)
		serviceControl.stop()
	}

	private fun handlePreparationException(preparationException: PreparationException) {
		logger.error("An error occurred during file preparation for file " + preparationException.positionedFile.serviceFile, preparationException)
		uncaughtExceptionHandler(preparationException.cause)
	}

	private fun handleMedia3PlaybackException(exception: androidx.media3.common.PlaybackException) {
		logger.error("A Media3 PlaybackException occurred")

		when (val cause = exception.cause) {
			is IllegalStateException -> {
				logger.error("The ExoPlayer player ended up in an illegal state, closing and restarting the player", cause)
				resetPlaylistManager(exception)
			}
			is NoSuchElementException -> {
				logger.error("The ExoPlayer player was unable to deque data, closing and restarting the player", cause)
				resetPlaylistManager(exception)
			}
			null -> serviceControl.stop()
			else -> uncaughtExceptionHandler(exception.cause)
		}
	}

	@OptIn(UnstableApi::class)
	private fun handleIoException(exception: IOException?) {
		when (exception) {
			is ParserException -> {
				logger.warn("A parser exception occurred while preparing the file, skipping playback", exception)
				changePlaylistPosition.skipToNext().then { _ -> changePlaybackState.resume() }
				return
			}
			is HttpDataSource.InvalidResponseCodeException -> {
				if (exception.responseCode == 416) {
					logger.warn("Received an error code of " + exception.responseCode + ", will attempt restarting the player", exception)
					resetPlaylistManager(exception)
					return
				}
			}
			is HttpDataSource.HttpDataSourceException -> {
				when (val httpCause = exception.cause) {
					is ProtocolException -> {
						if (httpCause.message == "unexpected end of stream") {
							logger.warn("The stream ended unexpectedly, skipping playback", exception)
							changePlaylistPosition.skipToNext().then { _ -> changePlaybackState.resume() }
							return
						}
					}
				}
			}
		}

		logger.error("An unexpected IO exception occurred during playback", exception)
		handleDisconnection(exception)
	}

	private fun handlePlaybackException(exception: PlaybackException) {
		when (val cause = exception.cause) {
			is ExoPlaybackException -> handleMedia3PlaybackException(cause)
			is IllegalStateException -> {
				logger.error("The player ended up in an illegal state - closing and restarting the player", exception)
				resetPlaylistManager(exception)
			}
			is IOException -> handleIoException(cause)
			null -> logger.error("An unexpected playback exception occurred", exception)
			else -> uncaughtExceptionHandler(cause)
		}
	}

	private fun handleTimeoutException(exception: PlaybackResourceNotAvailableInTimeException) {
		logger.warn("A timeout occurred during playback, will attempt restarting the player", exception)
		resetPlaylistManager(exception)
	}

	private fun resetPlaylistManager(error: Throwable) {
		if (errorLatch.trigger()) {
			logger.error("$numberOfErrors occurred within $errorLatchResetDuration, stopping the playback service. Last error: ${error.message}", error)
			exceptionAnnouncer.announce(error)
			serviceControl.stop()
			return
		}

		resetPlaybackService.resetPlaylistManager()
	}
}
