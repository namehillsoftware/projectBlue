package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.UpdateFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ProvideImages
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.CancellationException
import kotlin.math.roundToInt

class NowPlayingViewModel(
	private val messages: RegisterForMessages,
	private val nowPlayingRepository: INowPlayingRepository,
	private val selectedConnectionProvider: ProvideSelectedConnection,
	private val imageProvider: ProvideImages,
	private val fileProperties: ProvideScopedFileProperties,
	private val updateFileProperties: UpdateFileProperties,
	private val checkAuthentication: CheckIfScopedConnectionIsReadOnly,
	private val playbackService: ControlPlaybackService,
	private val stringResources: GetStringResources
) : ViewModel(), Closeable {

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(NowPlayingViewModel::class.java) }
		private val screenControlVisibilityTime by lazy { Duration.standardSeconds(5) }
	}

	private val onPlaybackStartedReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			isPlayingState.value = true
			updateKeepScreenOnStatus()
		}
	}

	private val onPlaybackStoppedReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			isPlayingState.value = false
			updateKeepScreenOnStatus()
		}
	}

	private val onPlaybackChangedReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			setView()
			showNowPlayingControls()
		}
	}

	private val onPlaylistChangedReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			nowPlayingRepository.nowPlaying
				.then { np ->
					nowPlayingListState.value = np.playlist.mapIndexed(::PositionedFile)
					setView()
				}
		}
	}

	private val onTrackPositionChanged = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val fileDuration = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.fileDuration, -1)
			if (fileDuration > -1) setTrackDuration(fileDuration)
			val filePosition = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1)
			if (filePosition > -1) setTrackProgress(filePosition)
		}
	}

	private var cachedPromises: CachedPromises? = null
	private var ratingUpdateJob: Job? = null

	private val filePositionState = MutableStateFlow(0)
	private val fileDurationState = MutableStateFlow(0)
	private val isPlayingState = MutableStateFlow(false)
	private val isReadOnlyState = MutableStateFlow(false)
	private val isNowPlayingImageLoadingState = MutableStateFlow(false)
	private val artistState = MutableStateFlow<String?>(stringResources.defaultArtist)
	private val titleState = MutableStateFlow<String?>(stringResources.defaultTitle)
	private val nowPlayingImageState = MutableStateFlow<Bitmap?>(null)
	private val songRatingState = MutableStateFlow(0F)
	private val isSongRatingEnabledState = MutableStateFlow(false)
	private val nowPlayingListState = MutableStateFlow(emptyList<PositionedFile>())
	private val nowPlayingFileState = MutableStateFlow<PositionedFile?>(null)
	private val isScreenOnEnabledState = MutableStateFlow(false)
	private val isScreenOnState = MutableStateFlow(false)
	private val isScreenControlsVisibleState = MutableStateFlow(false)
	private val isRepeatingState = MutableStateFlow(false)

	val filePosition = filePositionState.asStateFlow()
	val fileDuration = fileDurationState.asStateFlow()
	val isPlaying = isPlayingState.asStateFlow()
	val isReadOnly = isReadOnlyState.asStateFlow()
	val isNowPlayingImageLoading = isNowPlayingImageLoadingState.asStateFlow()
	val artist = artistState.asStateFlow()
	val title = titleState.asStateFlow()
	val nowPlayingImage = nowPlayingImageState.asStateFlow()
	val songRating = songRatingState
	val isSongRatingEnabled = isSongRatingEnabledState.asStateFlow()
	val nowPlayingList = nowPlayingListState.asStateFlow()
	val nowPlayingFile = nowPlayingFileState.asStateFlow()
	val isScreenOnEnabled = isScreenOnEnabledState.asStateFlow()
	val isScreenOn = isScreenOnState.asStateFlow()
	val isScreenControlsVisible = isScreenControlsVisibleState.asStateFlow()
	val isRepeating = isRepeatingState.asStateFlow()

	override fun close() {
		cachedPromises?.release()
	}

	fun initializeViewModel() {
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

		isPlayingState.value = false
		nowPlayingRepository
			.nowPlaying
			.eventually { np ->
				isRepeatingState.value = np.isRepeating
				selectedConnectionProvider
					.promiseSessionConnection()
					.then { connectionProvider ->
						nowPlayingListState.value = np.playlist.mapIndexed(::PositionedFile)
						np.playingFile?.also { serviceFile ->
							val filePosition = connectionProvider?.urlProvider?.baseUrl
								?.let { baseUrl ->
									if (cachedPromises?.urlKeyHolder == UrlKeyHolder(baseUrl, serviceFile)) filePositionState.value
									else np.filePosition
								}
								?: np.filePosition
							setView(serviceFile, filePosition)
						}
					}
			}
			.excuse { error -> logger.warn("An error occurred initializing `NowPlayingActivity`", error) }

		playbackService.promiseIsMarkedForPlay().then(::togglePlaying)
	}

	fun togglePlaying(isPlaying: Boolean) {
		isPlayingState.value = isPlaying
	}

	fun toggleScreenOn() {
		isScreenOnEnabledState.value = !isScreenOnEnabledState.value
		updateKeepScreenOnStatus()
	}

	fun showNowPlayingControls() {
		isScreenControlsVisibleState.value = true
		PromiseDelay
			.delay<Any?>(screenControlVisibilityTime)
			.then { isScreenControlsVisibleState.value = false }
	}

	fun toggleRepeating() {
		isRepeatingState.value = !isRepeatingState.value
		if (isRepeatingState.value) playbackService.setRepeating()
		else playbackService.setCompleting()
	}

	private fun setView() {
		nowPlayingRepository.nowPlaying
			.then { np ->
				np.playingFile?.let { serviceFile ->
					selectedConnectionProvider
						.promiseSessionConnection()
						.then { connectionProvider ->
							connectionProvider
								?.urlProvider
								?.baseUrl
								?.let { baseUrl ->
									if (cachedPromises?.urlKeyHolder == UrlKeyHolder(baseUrl, serviceFile)) filePositionState.value
									else 0
								}
								?.also { filePosition ->
									setView(serviceFile, filePosition)
								}
						}
				}
			}
	}

	private fun setView(serviceFile: ServiceFile, initialFilePosition: Number) {
		fun setNowPlayingImage(cachedPromises: CachedPromises) {
			isNowPlayingImageLoadingState.value = true
			cachedPromises
				.promisedImage
				.then { bitmap ->
					if (this.cachedPromises?.urlKeyHolder == cachedPromises.urlKeyHolder) {
						nowPlayingImageState.value = bitmap
						isNowPlayingImageLoadingState.value = false
					}
				}
				.excuse { e ->
					if (e is CancellationException)	logger.info("Bitmap retrieval cancelled", e)
					else logger.error("There was an error retrieving the image for serviceFile $serviceFile", e)
				}
		}

		fun setFileProperties(fileProperties: Map<String, String>, isReadOnly: Boolean) {
			artistState.value = fileProperties[KnownFileProperties.ARTIST]
			titleState.value = fileProperties[KnownFileProperties.NAME]

			val duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties)
			setTrackDuration(if (duration > 0) duration.toLong() else 100.toLong())
			setTrackProgress(initialFilePosition)

			val stringRating = fileProperties[KnownFileProperties.RATING]
			val fileRating = stringRating?.toFloatOrNull() ?: 0f

			ratingUpdateJob?.cancel()

			isReadOnlyState.value = isReadOnly
			songRatingState.value = fileRating

			ratingUpdateJob = songRatingState.onEach { newRating ->
				val ratingToString = newRating.roundToInt().toString()
				updateFileProperties
					.promiseFileUpdate(serviceFile, KnownFileProperties.RATING, ratingToString, false)
//					.eventuallyExcuse(com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.response(::handleIoException, messageHandler))
			}.launchIn(viewModelScope)
		}

		fun disableViewWithMessage() {
			titleState.value = stringResources.loading
			artistState.value = ""

			songRatingState.value = 0F
			isSongRatingEnabledState.value = false
		}

		fun handleException(exception: Throwable) {
//			val isIoException = handleIoException(exception)
//			if (!isIoException) return
//
//			PollConnectionService.pollSessionConnection(this).then {
//				if (serviceFile == NowPlayingActivity.viewStructure?.serviceFile) {
//					promisedNowPlayingImage?.cancel()
//					promisedNowPlayingImage = null
//				}
//				setView(serviceFile, initialFilePosition)
//			}
//			WaitForConnectionDialog.show(this)
		}

		selectedConnectionProvider.promiseSessionConnection()
			.then { connectionProvider ->
				val baseUrl = connectionProvider?.urlProvider?.baseUrl ?: return@then

				val urlKeyHolder = UrlKeyHolder(baseUrl, serviceFile)
				val currentCachedPromises = cachedPromises
					?.takeIf { urlKeyHolder == urlKeyHolder }
					?: run {
						cachedPromises?.release()
						CachedPromises(
							urlKeyHolder,
							checkAuthentication.promiseIsReadOnly(),
							fileProperties.promiseFileProperties(serviceFile),
							imageProvider.promiseFileBitmap(serviceFile)
						).also { cachedPromises = it }
					}

				setNowPlayingImage(currentCachedPromises)

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
//					.eventuallyExcuse(LoopedInPromise.response(::handleException, messageHandler))
			}
	}

	private fun updateKeepScreenOnStatus() {
		isScreenOnState.value = isPlayingState.value && isScreenOnEnabledState.value
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
		val promisedImage: Promise<Bitmap?>
	) {
		fun release() {
			promisedIsReadOnly.cancel()
			promisedProperties.cancel()
			promisedImage.cancel()
		}
	}
}
