package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.UpdateScopedFileProperties
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.polling.PollForConnections
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
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
	private val applicationMessages: RegisterForApplicationMessages,
	private val nowPlayingRepository: GetNowPlayingState,
	private val selectedConnectionProvider: ProvideSelectedConnection,
	private val fileProperties: ProvideScopedFileProperties,
	private val updateScopedFileProperties: UpdateScopedFileProperties,
	private val checkAuthentication: CheckIfScopedConnectionIsReadOnly,
	private val playbackService: ControlPlaybackService,
	private val pollConnections: PollForConnections,
	private val stringResources: GetStringResources
) : ViewModel()
{
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

	private val onPropertiesChangedSubscription = applicationMessages.registerReceiver { message: FilePropertyUpdatedMessage ->
		selectedConnectionProvider.promiseSessionConnection()
			.eventually { connectionProvider ->
				connectionProvider
					?.urlProvider
					?.baseUrl
					?.let { UrlKeyHolder(it, message.serviceFile) }
					?.takeIf { cachedPromises?.urlKeyHolder == it }
					?.let { urlKeyHolder ->
						val currentCachedPromises = synchronized(cachedPromiseSync) {
							cachedPromises?.close()
							CachedPromises(
								urlKeyHolder,
								checkAuthentication.promiseIsReadOnly(),
								fileProperties.promiseFileProperties(message.serviceFile),
							).also { cachedPromises = it }
						}

						currentCachedPromises
							.promisedProperties
							.eventually { fileProperties ->
								if (cachedPromises?.urlKeyHolder != urlKeyHolder) Unit.toPromise()
								else currentCachedPromises.promisedIsReadOnly.then { isReadOnly ->
									if (cachedPromises?.urlKeyHolder == urlKeyHolder) {
										setFileProperties(fileProperties, isReadOnly)
									}
								}
							}
					}
					.keepPromise(Unit)
			}
			.excuse { exception -> handleException(exception) }
	}

	private var cachedPromises: CachedPromises? = null
	private var controlsShownPromise = Promise.empty<Any?>()

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
		val serviceFile = nowPlayingFileState.value?.serviceFile ?: return

		songRatingState.value = rating
		val ratingToString = rating.roundToInt().toString()
		updateScopedFileProperties
			.promiseFileUpdate(serviceFile, KnownFileProperties.RATING, ratingToString, false)
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

	private fun updateViewFromRepository() =
		nowPlayingRepository.promiseNowPlaying()
			.then { np ->
				nowPlayingFileState.value = np?.playingFile
				np?.playingFile?.let { positionedFile ->
					selectedConnectionProvider
						.promiseSessionConnection()
						.then { connectionProvider ->
							connectionProvider
								?.urlProvider
								?.baseUrl
								?.let { baseUrl ->
									if (cachedPromises?.urlKeyHolder == UrlKeyHolder(baseUrl, positionedFile.serviceFile)) filePositionState.value
									else np.filePosition
								}
								?.also { filePosition ->
									setView(positionedFile.serviceFile, filePosition)
								}
						}
				}
				Unit
			}

	private fun resetView() {
		titleState.value = stringResources.loading
		artistState.value = ""

		isSongRatingEnabledState.value = false
		songRatingState.value = 0F

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

	private fun setView(serviceFile: ServiceFile, initialFilePosition: Number) {

		selectedConnectionProvider.promiseSessionConnection()
			.then { connectionProvider ->
				val baseUrl = connectionProvider?.urlProvider?.baseUrl ?: return@then

				val urlKeyHolder = UrlKeyHolder(baseUrl, serviceFile)

				val currentCachedPromises = synchronized(cachedPromiseSync) {
					if (cachedPromises?.urlKeyHolder == urlKeyHolder) return@then

					cachedPromises?.close()
					CachedPromises(
						urlKeyHolder,
						checkAuthentication.promiseIsReadOnly(),
						fileProperties.promiseFileProperties(serviceFile),
					).also { cachedPromises = it }
				}

				resetView()
				currentCachedPromises
					.promisedProperties
					.eventually { fileProperties ->
						if (cachedPromises?.urlKeyHolder != urlKeyHolder) Unit.toPromise()
						else currentCachedPromises.promisedIsReadOnly.then { isReadOnly ->
							if (cachedPromises?.urlKeyHolder == urlKeyHolder) {
								setFileProperties(fileProperties, isReadOnly)

								if (filePositionState.value == 0)
									setTrackProgress(initialFilePosition)
							}
						}
					}
					.excuse { exception -> handleException(exception) }
			}
	}

	private fun setFileProperties(fileProperties: Map<String, String>, isReadOnly: Boolean) {
		artistState.value = fileProperties[KnownFileProperties.ARTIST]
		titleState.value = fileProperties[KnownFileProperties.NAME]

		val duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties)
		setTrackDuration(if (duration > 0) duration else Int.MAX_VALUE)

		val stringRating = fileProperties[KnownFileProperties.RATING]
		val fileRating = stringRating?.toFloatOrNull() ?: 0f

		isReadOnlyState.value = isReadOnly
		songRatingState.value = fileRating

		isSongRatingEnabledState.value = true
	}

	private fun setTrackDuration(duration: Number) {
		fileDurationState.value = duration.toInt()
	}

	private fun setTrackProgress(progress: Number) {
		filePositionState.value = progress.toInt()
	}

	private class CachedPromises(
		val urlKeyHolder: UrlKeyHolder<ServiceFile>,
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
