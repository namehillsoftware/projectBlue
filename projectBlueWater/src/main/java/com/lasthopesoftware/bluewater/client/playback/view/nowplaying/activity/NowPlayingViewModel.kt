package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ProvideImages
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.CancellationException

class NowPlayingViewModel(
	messages: RegisterForMessages,
	private val nowPlayingRepository: INowPlayingRepository,
	private val selectedConnectionProvider: ProvideSelectedConnection,
	private val imageProvider: ProvideImages,
	private val fileProperties: ProvideScopedFileProperties,
	private val checkAuthentication: CheckIfScopedConnectionIsReadOnly,
	private val stringResources: GetStringResources
) : ViewModel(), Closeable {

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(NowPlayingViewModel::class.java) }
	}

	init {
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

	private val filePositionState = MutableStateFlow(0L)
	private val fileDurationState = MutableStateFlow(0L)
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

	val filePosition = filePositionState.asStateFlow()
	val fileDuration = fileDurationState.asStateFlow()
	val isPlaying = isPlayingState.asStateFlow()
	val isReadOnly = isReadOnlyState.asStateFlow()
	val isNowPlayingImageLoading = isNowPlayingImageLoadingState.asStateFlow()
	val isFilePropertiesReadOnly = isReadOnlyState.asStateFlow()
	val artist = artistState.asStateFlow()
	val title = titleState.asStateFlow()
	val nowPlayingImage = nowPlayingImageState.asStateFlow()
	val songRating = songRatingState.asStateFlow()
	val isSongRatingEnabled = songRatingState.asStateFlow()
	val nowPlayingList = nowPlayingListState.asStateFlow()
	val nowPlayingFile = nowPlayingFileState.asStateFlow()
	val isScreenOnEnabled = isScreenOnEnabledState.asStateFlow()
	val isScreenOn = isScreenOnState.asStateFlow()
	val isScreenControlsVisible = isScreenControlsVisibleState.asStateFlow()

	override fun close() {
		cachedPromises?.release()
	}

	fun initializeViewModel() {
		isPlayingState.value = false
		nowPlayingRepository
			.nowPlaying
			.eventually { np ->
				selectedConnectionProvider
					.promiseSessionConnection()
					.then { connectionProvider ->
						val serviceFile = np.playlist[np.playlistPosition]
						val filePosition = connectionProvider?.urlProvider?.baseUrl
							?.let { baseUrl ->
								if (cachedPromises?.urlKeyHolder == UrlKeyHolder(baseUrl, serviceFile)) filePositionState.value
								else np.filePosition
							}
							?: np.filePosition
						setView(serviceFile, filePosition)
					}
			}
//			}
//			.excuse { error -> NowPlayingActivity.logger.warn("An error occurred initializing `NowPlayingActivity`", error) }

//		PlaybackService.promiseIsMarkedForPlay(this).then(::togglePlayingButtons)
	}

	private fun setView() {
		nowPlayingRepository.nowPlaying
			.then { np ->
				np.playingFile?.let { serviceFile ->
					selectedConnectionProvider
						.promiseSessionConnection()
						.then { connectionProvider ->
							connectionProvider?.urlProvider?.baseUrl?.let { baseUrl ->
								val filePosition =
									if (cachedPromises?.urlKeyHolder == UrlKeyHolder(baseUrl, serviceFile)) filePositionState.value
									else 0
								setView(serviceFile, filePosition)
							}
						}
				}
			}
	}

	private fun setView(serviceFile: ServiceFile, initialFilePosition: Long) {
		fun setNowPlayingImage(cachedPromises: CachedPromises) {
			isNowPlayingImageLoadingState.value = true
			cachedPromises
				.promisedImage
				.then { bitmap ->
					if (this.cachedPromises?.urlKeyHolder != cachedPromises.urlKeyHolder) Unit
					else {
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

			isReadOnlyState.value = isReadOnly
			songRatingState.value = fileRating

//			with (miniSongRating.findView()) {
//				rating = fileRating
//				isEnabled = !isReadOnly
//
//				onRatingBarChangeListener =
//					if (isReadOnly) null
//					else OnRatingBarChangeListener { _, newRating, fromUser ->
//						if (fromUser) {
//							songRating.findView().rating = newRating
//							val ratingToString = newRating.roundToInt().toString()
//							filePropertiesStorage
//								.promiseFileUpdate(serviceFile, com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties.RATING, ratingToString, false)
//								.eventuallyExcuse(com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.response(::handleIoException, messageHandler))
//							com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.NowPlayingActivity.viewStructure?.fileProperties?.put(
//								com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties.RATING, ratingToString)
//						}
//					}
//			}
//
//			with (songRating.findView()) {
//				rating = fileRating
//				isEnabled = !isReadOnly
//
//				onRatingBarChangeListener =
//					if (isReadOnly) null
//					else OnRatingBarChangeListener { _, newRating, fromUser ->
//						if (fromUser && nowPlayingToggledVisibilityControls.isVisible) {
//							miniSongRating.findView().rating = newRating
//							val ratingToString = newRating.roundToInt().toString()
//							filePropertiesStorage
//								.promiseFileUpdate(serviceFile, com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties.RATING, ratingToString, false)
//								.eventuallyExcuse(com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.response(::handleIoException, messageHandler))
//							com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.NowPlayingActivity.viewStructure?.fileProperties?.put(
//								com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties.RATING, ratingToString)
//						}
//					}
//			}
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
							if (cachedPromises?.urlKeyHolder != urlKeyHolder) Unit.toPromise()
							else setFileProperties(fileProperties, isReadOnly)
						}
					}
//					.eventuallyExcuse(LoopedInPromise.response(::handleException, messageHandler))
			}
	}

	private fun updateKeepScreenOnStatus() {
		isScreenOnState.value = isPlayingState.value && isScreenOnEnabledState.value
	}

	private fun showNowPlayingControls() {
		isScreenControlsVisibleState.value = true
		PromiseDelay
			.delay<Any?>(Duration.standardSeconds(5))
			.then { isScreenControlsVisibleState.value = false }
	}

	private fun setTrackDuration(duration: Long) {
		fileDurationState.value = duration
	}

	private fun setTrackProgress(progress: Long) {
		filePositionState.value = progress
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