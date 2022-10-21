package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.UpdateFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.connection.polling.PollForConnections
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.joda.time.Duration
import kotlin.math.roundToInt

private val screenControlVisibilityTime by lazy { Duration.standardSeconds(5) }

class NowPlayingFilePropertiesViewModel(
	applicationMessages: RegisterForApplicationMessages,
	private val nowPlayingRepository: GetNowPlayingState,
	private val fileProperties: ProvideLibraryFileProperties,
	private val provideUrlKey: ProvideUrlKey,
	private val updateFileProperties: UpdateFileProperties,
	private val checkAuthentication: CheckIfConnectionIsReadOnly,
	private val playbackService: ControlPlaybackService,
	private val pollConnections: PollForConnections,
	private val stringResources: GetStringResources,
) : ViewModel()
{

	@Volatile
	private var activeSongRatingUpdates = 0
	private var cachedPromises: CachedPromises? = null
	private var controlsShownPromise = Promise.empty<Any?>()

	private val onPlaybackStartedSubscription =
		applicationMessages.registerReceiver { _: PlaybackMessage.PlaybackStarted -> togglePlaying(true) }

	private val onPlaybackStoppedSubscription = applicationMessages.run {
		val onPlaybackStopped = { _: PlaybackMessage -> togglePlaying(false) }
		registerForClass(cls<PlaybackMessage.PlaybackPaused>(), onPlaybackStopped)
		registerForClass(cls<PlaybackMessage.PlaybackInterrupted>(), onPlaybackStopped)
		registerForClass(cls<PlaybackMessage.PlaybackStopped>(), onPlaybackStopped)
	}

	private val onPlaybackChangedSubscription = applicationMessages.registerReceiver { _: PlaybackMessage.TrackChanged ->
		updateViewFromRepository()
		showNowPlayingControls()
	}

	private val onPlaylistChangedSubscription =
		applicationMessages.registerReceiver { _: PlaybackMessage.PlaylistChanged -> updateViewFromRepository(); Unit }

	private val onTrackPositionChangedSubscription = applicationMessages.registerReceiver { update: TrackPositionUpdate ->
		setTrackDuration(update.fileDuration.millis)
		setTrackProgress(update.filePosition.millis)
	}

	private val onPropertiesChangedSubscription = applicationMessages.registerReceiver(::handleFilePropertyUpdates)

	private val cachedPromiseSync = Any()
	private val filePositionState = MutableStateFlow(0)
	private val fileDurationState = MutableStateFlow(Int.MAX_VALUE) // Use max so that position updates will take effect
	private val isPlayingState = MutableStateFlow(false)
	private val isReadOnlyState = MutableStateFlow(false)
	private val artistState = MutableStateFlow<String?>(stringResources.defaultNowPlayingArtist)
	private val titleState = MutableStateFlow<String?>(stringResources.defaultNowPlayingTrackTitle)
	private val songRatingState = MutableStateFlow(0F)
	private val isSongRatingEnabledState = MutableStateFlow(false)
	private val nowPlayingFileState = MutableStateFlow<PositionedFile?>(null)
	private val isScreenControlsVisibleState = MutableStateFlow(false)
	private val isRepeatingState = MutableStateFlow(false)
	private val unexpectedErrorState = MutableStateFlow<Throwable?>(null)

	val filePosition = filePositionState.asStateFlow()
	val fileDuration = fileDurationState.asStateFlow()
	val isPlaying = isPlayingState.asStateFlow()
	val isReadOnly = isReadOnlyState.asStateFlow()
	val artist = artistState.asStateFlow()
	val title = titleState.asStateFlow()
	val songRating = songRatingState.asStateFlow()
	val isSongRatingEnabled = isSongRatingEnabledState.asStateFlow()
	val nowPlayingFile = nowPlayingFileState.asStateFlow()
	val isScreenControlsVisible = isScreenControlsVisibleState.asStateFlow()
	val isRepeating = isRepeatingState.asStateFlow()
	val unexpectedError = unexpectedErrorState.asStateFlow()

	override fun onCleared() {
		cachedPromises?.close()

		onTrackPositionChangedSubscription.close()
		onPlaybackChangedSubscription.close()
		onPlaybackStartedSubscription.close()
		onPlaylistChangedSubscription.close()
		onPlaybackStoppedSubscription.close()
		onPropertiesChangedSubscription.close()

		controlsShownPromise.cancel()
	}

	fun initializeViewModel(): Promise<Unit> {
		togglePlaying(false)
		val nowPlayingPromise = nowPlayingRepository
			.promiseNowPlaying()
			.then { np -> isRepeatingState.value = np?.isRepeating ?: false }

		val promisedViewUpdate = updateViewFromRepository()

		val promisedTogglePlayingUpdate = playbackService.promiseIsMarkedForPlay().then(::togglePlaying)

		return Promise.whenAll(nowPlayingPromise, promisedViewUpdate, promisedTogglePlayingUpdate).unitResponse()
	}

	fun togglePlaying(isPlaying: Boolean) {
		isPlayingState.value = isPlaying
	}

	fun updateRating(rating: Float) {
		if (!isSongRatingEnabledState.value) return

		val libraryId = cachedPromises?.libraryId ?: return
		val serviceFile = cachedPromises?.serviceFile ?: return

		songRatingState.value = rating

		activeSongRatingUpdates++
		updateFileProperties
			.promiseFileUpdate(
				libraryId,
				serviceFile,
				KnownFileProperties.Rating,
				rating.roundToInt().toString(),
				false)
			.must { activeSongRatingUpdates = activeSongRatingUpdates.dec().coerceAtLeast(0) }
			.excuse(::handleIoException)
	}

	@Synchronized
	fun showNowPlayingControls() {
		controlsShownPromise.cancel()

		isScreenControlsVisibleState.value = true
		controlsShownPromise = CancellableProxyPromise { cp ->
			val promisedDelay = PromiseDelay.delay<Any?>(screenControlVisibilityTime)
			promisedDelay.then {
				if (!cp.isCancelled)
					isScreenControlsVisibleState.value = false
			}
			promisedDelay
		}
	}

	fun toggleRepeating() {
		with (isRepeatingState) {
			value = !value
			if (value) playbackService.setRepeating()
			else playbackService.setCompleting()
		}
	}

	private fun updateViewFromRepository(): Promise<Unit> =
		nowPlayingRepository.promiseNowPlaying()
			.eventually { np ->
				nowPlayingFileState.value = np?.playingFile
				np?.playingFile?.let { positionedFile ->
					provideUrlKey
						.promiseUrlKey(np.libraryId, positionedFile.serviceFile)
						.eventually { key ->
							key
								?.let {
									if (cachedPromises?.key == key) filePositionState.value
									else np.filePosition
								}
								?.let { filePosition ->
									setView(np.libraryId, positionedFile.serviceFile, filePosition)
								}
								.keepPromise(Unit)
						}
				}.keepPromise(Unit)
			}

	private fun resetView() {
		titleState.value = stringResources.loading
		artistState.value = ""

		isSongRatingEnabledState.value = false
		songRatingState.value = 0F

		activeSongRatingUpdates = 0

		setTrackProgress(0)
	}

	private fun handleException(exception: Throwable) {
		val isIoException = handleIoException(exception)
		if (!isIoException) return

		unexpectedErrorState.value = exception
		pollConnections.pollSessionConnection().then {
			synchronized(cachedPromiseSync) {
				cachedPromises?.close()
				cachedPromises = null
			}
			updateViewFromRepository()
		}
	}

	private fun handleIoException(exception: Throwable) =
		if (ConnectionLostExceptionFilter.isConnectionLostException(exception)) true
		else {
			unexpectedErrorState.value = exception
			false
		}

	private fun setView(libraryId: LibraryId, serviceFile: ServiceFile, initialFilePosition: Number) =
		provideUrlKey
			.promiseUrlKey(libraryId, serviceFile)
			.eventually { key ->
				key ?: return@eventually Unit.toPromise()

				val currentCachedPromises = synchronized(cachedPromiseSync) {
					if (cachedPromises?.key == key) return@eventually Unit.toPromise()

					cachedPromises?.close()
					CachedPromises(
						key,
						libraryId,
						serviceFile,
						checkAuthentication.promiseIsReadOnly(libraryId),
						fileProperties.promiseFileProperties(libraryId, serviceFile),
					).also { cachedPromises = it }
				}

				resetView()
				currentCachedPromises
					.promisedProperties
					.eventually { fileProperties ->
						if (cachedPromises?.key != key) Unit.toPromise()
						else currentCachedPromises.promisedIsReadOnly.then { isReadOnly ->
							if (cachedPromises?.key == key) {
								setFileProperties(fileProperties, isReadOnly)

								if (filePositionState.value == 0)
									setTrackProgress(initialFilePosition)
							}
						}
					}
					.apply { excuse(::handleException) }
			}

	private fun handleFilePropertyUpdates(message: FilePropertiesUpdatedMessage) {
		cachedPromises?.let { promises ->
			val key = message.urlServiceKey
			val libraryId = promises.libraryId
			val serviceFile = promises.serviceFile

			if (promises.key == message.urlServiceKey) {
				val currentCachedPromises = synchronized(cachedPromiseSync) {
					cachedPromises?.close()
					CachedPromises(
						key,
						promises.libraryId,
						promises.serviceFile,
						checkAuthentication.promiseIsReadOnly(libraryId),
						fileProperties.promiseFileProperties(libraryId, serviceFile),
					).also { cachedPromises = it }
				}

				currentCachedPromises
					.promisedProperties
					.eventually { fileProperties ->
						if (cachedPromises?.key != key) Unit.toPromise()
						else currentCachedPromises.promisedIsReadOnly.then { isReadOnly ->
							if (cachedPromises?.key == key)
								setFileProperties(fileProperties, isReadOnly)
						}
					}
					.excuse(::handleException)
			}
		}
	}

	private fun setFileProperties(fileProperties: Map<String, String>, isReadOnly: Boolean) {
		artistState.value = fileProperties[KnownFileProperties.Artist]
		titleState.value = fileProperties[KnownFileProperties.Name]

		val duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties)
		setTrackDuration(if (duration > 0) duration else Long.MAX_VALUE)

		if (activeSongRatingUpdates == 0) {
			val stringRating = fileProperties[KnownFileProperties.Rating]
			val fileRating = stringRating?.toFloatOrNull() ?: 0f
			songRatingState.value = fileRating
		}

		isReadOnlyState.value = isReadOnly

		isSongRatingEnabledState.value = true
	}

	private fun setTrackDuration(duration: Number) {
		fileDurationState.value = duration.toInt()
	}

	private fun setTrackProgress(progress: Number) {
		filePositionState.value = progress.toInt()
	}

	private class CachedPromises(
		val key: UrlKeyHolder<ServiceFile>,
		val libraryId: LibraryId,
		val serviceFile: ServiceFile,
		val promisedIsReadOnly: Promise<Boolean>,
		val promisedProperties: Promise<Map<String, String>>,
	)
		: AutoCloseable
	{
		override fun close() {
			promisedIsReadOnly.cancel()
			promisedProperties.cancel()
		}
	}
}
