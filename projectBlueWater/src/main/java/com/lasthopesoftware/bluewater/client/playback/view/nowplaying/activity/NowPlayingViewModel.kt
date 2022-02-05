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
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ProvideImages
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionDialog
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.slf4j.LoggerFactory
import java.util.concurrent.CancellationException

class NowPlayingViewModel(
	messages: RegisterForMessages,
	private val nowPlayingRepository: INowPlayingRepository,
	private val selectedConnectionProvider: ProvideSelectedConnection,
	private val imageProvider: ProvideImages,
	private val stringResources: GetStringResources
) : ViewModel() {

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
		}
	}

	private val onPlaybackStoppedReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			isPlayingState.value = false
		}
	}

	private val onPlaybackChangedReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			nowPlayingRepository.nowPlaying
				.then { np ->
					np.playingFile?.let { serviceFile ->
						selectedConnectionProvider
							.promiseSessionConnection()
							.then { connectionProvider ->
								connectionProvider?.urlProvider?.baseUrl?.let { baseUrl ->
									val filePosition = NowPlayingActivity.viewStructure
										?.takeIf { it.urlKeyHolder == UrlKeyHolder(baseUrl, serviceFile) }
										?.filePosition
										?: 0
									setView(serviceFile, filePosition)
								}
							}
					}
				}
		}
	}

	private var currentServiceFileByUrl: UrlKeyHolder<ServiceFile>? = null
	private var promisedNowPlayingImage: Promise<Bitmap?>? = null

	private val filePropertiesState = MutableStateFlow<MutableMap<String, String>?>(null)
	private val filePositionState = MutableStateFlow(0L)
	private val fileDurationState = MutableStateFlow(0L)
	private val isPlayingState = MutableStateFlow(false)
	private val isReadOnlyState = MutableStateFlow(false)
	private val isNowPlayingImageLoadingState = MutableStateFlow(false)
	private val artistState = MutableStateFlow<String?>(null)
	private val titleState = MutableStateFlow<String?>(null)
	private val nowPlayingImageState = MutableStateFlow<Bitmap?>(null)

	val fileProperties = filePropertiesState.asStateFlow()
	val filePosition = filePositionState.asStateFlow()
	val fileDuration = fileDurationState.asStateFlow()
	val isFilePropertiesReadOnly = isReadOnlyState.asStateFlow()
	val isNowPlayingImageLoading = isNowPlayingImageLoadingState.asStateFlow()
	val isPlaying = isPlayingState.asStateFlow()
	val artist = artistState.asStateFlow()
	val title = titleState.asStateFlow()

	fun release() {
		promisedNowPlayingImage?.cancel()
	}

	private fun setView(serviceFile: ServiceFile, initialFilePosition: Long) {
		fun setNowPlayingImage(serviceFileByUrl: UrlKeyHolder<ServiceFile>) {
			isNowPlayingImageLoadingState.value = true
			val promisedNowPlayingImage = this.promisedNowPlayingImage ?: imageProvider.promiseFileBitmap(serviceFile).also { promisedNowPlayingImage = it }
			promisedNowPlayingImage
				.then { bitmap ->
					if (currentServiceFileByUrl != serviceFileByUrl) Unit
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

			with (miniSongRating.findView()) {
				rating = fileRating
				isEnabled = !isReadOnly

				onRatingBarChangeListener =
					if (isReadOnly) null
					else OnRatingBarChangeListener { _, newRating, fromUser ->
						if (fromUser) {
							songRating.findView().rating = newRating
							val ratingToString = newRating.roundToInt().toString()
							filePropertiesStorage
								.promiseFileUpdate(serviceFile, com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties.RATING, ratingToString, false)
								.eventuallyExcuse(com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.response(::handleIoException, messageHandler))
							com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.NowPlayingActivity.viewStructure?.fileProperties?.put(
								com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties.RATING, ratingToString)
						}
					}
			}

			with (songRating.findView()) {
				rating = fileRating
				isEnabled = !isReadOnly

				onRatingBarChangeListener =
					if (isReadOnly) null
					else OnRatingBarChangeListener { _, newRating, fromUser ->
						if (fromUser && nowPlayingToggledVisibilityControls.isVisible) {
							miniSongRating.findView().rating = newRating
							val ratingToString = newRating.roundToInt().toString()
							filePropertiesStorage
								.promiseFileUpdate(serviceFile, com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties.RATING, ratingToString, false)
								.eventuallyExcuse(com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.response(::handleIoException, messageHandler))
							com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.NowPlayingActivity.viewStructure?.fileProperties?.put(
								com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties.RATING, ratingToString)
						}
					}
			}
		}

		fun disableViewWithMessage() {
			titleState.value = stringResources.loading
			artistState.value = ""

			songRating.findView().disableAndClear()
			miniSongRating.findView().disableAndClear()
		}

		fun handleException(exception: Throwable) {
			val isIoException = handleIoException(exception)
			if (!isIoException) return

			PollConnectionService.pollSessionConnection(this).then {
				if (serviceFile == NowPlayingActivity.viewStructure?.serviceFile) {
					promisedNowPlayingImage?.cancel()
					promisedNowPlayingImage = null
				}
				setView(serviceFile, initialFilePosition)
			}
			WaitForConnectionDialog.show(this)
		}

		selectedConnectionProvider.promiseSessionConnection()
			.then { connectionProvider ->
				val baseUrl = connectionProvider?.urlProvider?.baseUrl ?: return@then

				val urlKeyHolder = UrlKeyHolder(baseUrl, serviceFile)
				if (currentServiceFileByUrl != urlKeyHolder) {
					release()
				}

				val localViewStructure = NowPlayingActivity.viewStructure ?: NowPlayingActivity.ViewStructure(
					urlKeyHolder,
					serviceFile
				)
				NowPlayingActivity.viewStructure = localViewStructure

				setNowPlayingImage(localViewStructure)

				val cachedFileProperties = localViewStructure.fileProperties
				val isReadOnly = localViewStructure.isFilePropertiesReadOnly
				if (cachedFileProperties != null && isReadOnly != null) {
					setFileProperties(cachedFileProperties, isReadOnly)
					return@ImmediateResponse
				}

				disableViewWithMessage()
				val promisedIsConnectionReadOnly = lazySelectedConnectionAuthenticationChecker.promiseIsReadOnly()
				lazyFilePropertiesProvider
					.promiseFileProperties(serviceFile)
					.eventually { fileProperties ->
						if (localViewStructure !== NowPlayingActivity.viewStructure) Unit.toPromise()
						else promisedIsConnectionReadOnly.eventually { isReadOnly ->
							if (localViewStructure !== NowPlayingActivity.viewStructure) Unit.toPromise()
							else LoopedInPromise(MessageWriter {
								localViewStructure.fileProperties = fileProperties.toMutableMap()
								localViewStructure.isFilePropertiesReadOnly = isReadOnly
								setFileProperties(fileProperties, isReadOnly)
							}, messageHandler)
						}
					}
					.eventuallyExcuse(LoopedInPromise.response(::handleException, messageHandler))
			}
	}

	private fun setTrackDuration(duration: Long) {
		songProgressBar.findView().max = duration.toInt()
		miniSongProgressBar.findView().max = duration.toInt()
		NowPlayingActivity.viewStructure?.fileDuration = duration
	}

	private fun setTrackProgress(progress: Long) {
		songProgressBar.findView().progress = progress.toInt()
		miniSongProgressBar.findView().progress = progress.toInt()
		NowPlayingActivity.viewStructure?.filePosition = progress
	}
}
