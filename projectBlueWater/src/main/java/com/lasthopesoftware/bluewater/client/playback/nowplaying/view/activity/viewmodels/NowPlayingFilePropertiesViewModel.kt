package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels

import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.UpdateFileProperties
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.polling.PollForConnections
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

private val logger by lazy { LoggerFactory.getLogger(NowPlayingFilePropertiesViewModel::class.java) }
private val screenControlVisibilityTime by lazy { Duration.standardSeconds(5) }

class NowPlayingFilePropertiesViewModel(
	private val messages: RegisterForMessages,
	private val nowPlayingRepository: GetNowPlayingState,
	private val selectedConnectionProvider: ProvideSelectedConnection,
	private val fileProperties: ProvideScopedFileProperties,
	private val updateFileProperties: UpdateFileProperties,
	private val checkAuthentication: CheckIfScopedConnectionIsReadOnly,
	private val playbackService: ControlPlaybackService,
	private val pollConnections: PollForConnections,
	private val stringResources: GetStringResources,
	private val controlDrawerState: ControlDrawerState,
	private val controlScreenOnState: ControlScreenOnState
) : ViewModel(), ControlDrawerState by controlDrawerState, ControlScreenOnState by controlScreenOnState
{
	private val onPlaybackStartedReceiver: ReceiveBroadcastEvents
	private val onPlaybackStoppedReceiver: ReceiveBroadcastEvents
	private val onPlaybackChangedReceiver: ReceiveBroadcastEvents
	private val onPlaylistChangedReceiver: ReceiveBroadcastEvents
	private val onTrackPositionChanged: ReceiveBroadcastEvents

	private var cachedPromises: CachedPromises? = null
	private var controlsShownPromise = Promise.empty<Any?>()

	private val cachedPromiseSync = Any()
	private val filePositionState = MutableStateFlow(0)
	private val fileDurationState = MutableStateFlow(Int.MAX_VALUE) // Use max so that position updates will take effect
	private val isPlayingState = MutableStateFlow(false)
	private val isReadOnlyState = MutableStateFlow(false)
	private val artistState = MutableStateFlow<String?>(stringResources.defaultArtist)
	private val titleState = MutableStateFlow<String?>(stringResources.defaultTitle)
	private val songRatingState = MutableStateFlow(0F)
	private val isSongRatingEnabledState = MutableStateFlow(false)
	private val nowPlayingListState = MutableStateFlow(emptyList<PositionedFile>())
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
	val nowPlayingList = nowPlayingListState.asStateFlow()
	val nowPlayingFile = nowPlayingFileState.asStateFlow()
	val isScreenControlsVisible = isScreenControlsVisibleState.asStateFlow()
	val isRepeating = isRepeatingState.asStateFlow()
	val unexpectedError = unexpectedErrorState.asStateFlow()

	init {
		onPlaybackStartedReceiver = ReceiveBroadcastEvents { togglePlaying(true) }
		onPlaybackStoppedReceiver = ReceiveBroadcastEvents { togglePlaying(false) }
		onPlaylistChangedReceiver = ReceiveBroadcastEvents { updateViewFromRepository() }

		onPlaybackChangedReceiver = ReceiveBroadcastEvents {
			updateViewFromRepository()
			showNowPlayingControls()
		}

		onTrackPositionChanged =
			ReceiveBroadcastEvents { intent ->
				val fileDuration = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.fileDuration, -1)
				if (fileDuration > -1) setTrackDuration(fileDuration)
				val filePosition = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1)
				if (filePosition > -1) setTrackProgress(filePosition)
			}

		val playbackStoppedIntentFilter = IntentFilter().apply {
			addAction(PlaylistEvents.onPlaylistPause)
			addAction(PlaylistEvents.onPlaylistInterrupted)
			addAction(PlaylistEvents.onPlaylistStop)
		}

		with(messages) {
			registerReceiver(onPlaybackStoppedReceiver, playbackStoppedIntentFilter)
			registerReceiver(onPlaybackStartedReceiver, IntentFilter(PlaylistEvents.onPlaylistStart))
			registerReceiver(onPlaybackChangedReceiver, IntentFilter(PlaylistEvents.onPlaylistTrackChange))
			registerReceiver(onPlaylistChangedReceiver, IntentFilter(PlaylistEvents.onPlaylistChange))
			registerReceiver(onTrackPositionChanged, IntentFilter(TrackPositionBroadcaster.trackPositionUpdate))
		}
	}

	override fun onCleared() {
		cachedPromises?.close()
		with(messages) {
			unregisterReceiver(onPlaybackStoppedReceiver)
			unregisterReceiver(onPlaybackStartedReceiver)
			unregisterReceiver(onPlaybackChangedReceiver)
			unregisterReceiver(onPlaylistChangedReceiver)
			unregisterReceiver(onTrackPositionChanged)
		}
		controlsShownPromise.cancel()
	}

	fun initializeViewModel() {
		togglePlaying(false)
		nowPlayingRepository
			.promiseNowPlaying()
			.then { np -> isRepeatingState.value = np?.isRepeating ?: false }
			.excuse { error -> logger.warn("An error occurred initializing `NowPlayingActivity`", error) }

		updateViewFromRepository()

		playbackService.promiseIsMarkedForPlay().then(::togglePlaying)
	}

	fun togglePlaying(isPlaying: Boolean) {
		isPlayingState.value = isPlaying
	}

	fun updateRating(rating: Float) {
		if (!isSongRatingEnabledState.value) return
		val serviceFile = nowPlayingFileState.value?.serviceFile ?: return

		songRatingState.value = rating
		val ratingToString = rating.roundToInt().toString()
		updateFileProperties
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

	private fun updateViewFromRepository() {
		nowPlayingRepository.promiseNowPlaying()
			.then { np ->
				nowPlayingListState.value = np?.playlist?.mapIndexed(::PositionedFile) ?: emptyList()
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
			}
	}

	private fun disableViewWithMessage() {
		titleState.value = stringResources.loading
		artistState.value = ""

		isSongRatingEnabledState.value = false
		songRatingState.value = 0F
	}

	private fun handleIoException(exception: Throwable) =
		if (ConnectionLostExceptionFilter.isConnectionLostException(exception)) true
		else {
			unexpectedErrorState.value = exception
			false
		}

	private fun setView(serviceFile: ServiceFile, initialFilePosition: Number) {

		fun handleException(exception: Throwable) {
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

		fun setFileProperties(fileProperties: Map<String, String>, isReadOnly: Boolean) {
			artistState.value = fileProperties[KnownFileProperties.ARTIST]
			titleState.value = fileProperties[KnownFileProperties.NAME]

			val duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties)
			setTrackDuration(if (duration > 0) duration else Int.MAX_VALUE)
			setTrackProgress(initialFilePosition)

			val stringRating = fileProperties[KnownFileProperties.RATING]
			val fileRating = stringRating?.toFloatOrNull() ?: 0f

			isReadOnlyState.value = isReadOnly
			songRatingState.value = fileRating

			isSongRatingEnabledState.value = true
		}

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

				disableViewWithMessage()
				currentCachedPromises
					.promisedProperties
					.eventually { fileProperties ->
						if (cachedPromises?.urlKeyHolder != urlKeyHolder) Unit.toPromise()
						else currentCachedPromises.promisedIsReadOnly.then { isReadOnly ->
							if (cachedPromises?.urlKeyHolder == urlKeyHolder)
								setFileProperties(fileProperties, isReadOnly)
						}
					}
					.excuse { exception -> handleException(exception) }
			}
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