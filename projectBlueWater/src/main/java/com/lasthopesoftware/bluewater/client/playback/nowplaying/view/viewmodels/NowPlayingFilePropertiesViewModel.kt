package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.UpdateFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyNotReturnedException
import com.lasthopesoftware.bluewater.client.connection.polling.PollForLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.LibraryConnectionChangedMessage
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.promiseReceivedMessage
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.Companion.forward
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import kotlin.math.roundToInt

private val logger by lazyLogger<NowPlayingFilePropertiesViewModel>()
private val screenControlVisibilityTime by lazy { Duration.standardSeconds(5) }

class NowPlayingFilePropertiesViewModel(
	private val applicationMessages: RegisterForApplicationMessages,
	private val nowPlayingRepository: GetNowPlayingState,
	private val fileProperties: ProvideFreshLibraryFileProperties,
	private val provideUrlKey: ProvideUrlKey,
	private val updateFileProperties: UpdateFileProperties,
	private val checkAuthentication: CheckIfConnectionIsReadOnly,
	private val playbackService: ControlPlaybackService,
	private val pollConnections: PollForLibraryConnections,
	private val stringResources: GetStringResources,
) : ViewModel()
{

	@Volatile
	private var activeSongRatingUpdates = 0
	private var cachedPromises: CachedPromises? = null
	private var controlsShownPromise = Promise.empty<Any?>()
	private var promisedConnectionChanged: Promise<*> = Promise.empty<Any?>()

	private val onPlaybackStartedSubscription =
		applicationMessages.registerReceiver { _: PlaybackMessage.PlaybackStarted -> togglePlaying(true) }

	private val onPlaybackStoppedSubscription = applicationMessages.run {
		val onPlaybackStopped = { _: PlaybackMessage -> togglePlaying(false) }
		registerForClass(cls<PlaybackMessage.PlaybackPaused>(), onPlaybackStopped)
		registerForClass(cls<PlaybackMessage.PlaybackInterrupted>(), onPlaybackStopped)
		registerForClass(cls<PlaybackMessage.PlaybackStopped>(), onPlaybackStopped)
	}

	private val onTrackChangedSubscription = applicationMessages.registerReceiver { m: LibraryPlaybackMessage.TrackChanged ->
		if (m.libraryId == activeLibraryId.value) {
			updateViewFromRepository(m.libraryId)
			showNowPlayingControls()
		}
	}

	private val onPlaylistChangedSubscription =
		applicationMessages.registerReceiver { _: LibraryPlaybackMessage.PlaylistChanged ->
			activeLibraryId.value?.apply(::updateViewFromRepository)
		}

	private val onTrackPositionChangedSubscription = applicationMessages.registerReceiver { update: TrackPositionUpdate ->
		setTrackDuration(update.fileDuration.millis)
		setTrackProgress(update.filePosition.millis)
	}

	private val onPropertiesChangedSubscription = applicationMessages.registerReceiver(::handleFilePropertyUpdates)

	private val cachedPromiseSync = Any()
	private val filePositionState = MutableInteractionState(0)
	private val fileDurationState = MutableInteractionState(Int.MAX_VALUE) // Use max so that position updates will take effect
	private val isPlayingState = MutableInteractionState(false)
	private val isReadOnlyState = MutableInteractionState(false)
	private val artistState = MutableInteractionState(stringResources.defaultNowPlayingArtist)
	private val titleState = MutableInteractionState(stringResources.defaultNowPlayingTrackTitle)
	private val songRatingState = MutableInteractionState(0F)
	private val isSongRatingEnabledState = MutableInteractionState(false)
	private val nowPlayingFileState = MutableInteractionState<PositionedFile?>(null)
	private val isScreenControlsVisibleState = MutableInteractionState(false)
	private val unexpectedErrorState = MutableInteractionState<Throwable?>(null)
	private val activeLibraryIdState = MutableInteractionState<LibraryId?>(null)

	val filePosition = filePositionState.asReadOnly()
	val fileDuration = fileDurationState.asReadOnly()
	val isPlaying = isPlayingState.asReadOnly()
	val isReadOnly = isReadOnlyState.asReadOnly()
	val artist = artistState.asReadOnly()
	val title = titleState.asReadOnly()
	val songRating = songRatingState.asReadOnly()
	val isSongRatingEnabled = isSongRatingEnabledState.asReadOnly()
	val nowPlayingFile = nowPlayingFileState.asReadOnly()
	val isScreenControlsVisible = isScreenControlsVisibleState.asReadOnly()
	val unexpectedError = unexpectedErrorState.asReadOnly()
	val activeLibraryId = activeLibraryIdState.asReadOnly()

	override fun onCleared() {
		cachedPromises?.close()

		onTrackPositionChangedSubscription.close()
		onTrackChangedSubscription.close()
		onPlaybackStartedSubscription.close()
		onPlaylistChangedSubscription.close()
		onPlaybackStoppedSubscription.close()
		onPropertiesChangedSubscription.close()

		promisedConnectionChanged.cancel()
		controlsShownPromise.cancel()
	}

	fun initializeViewModel(libraryId: LibraryId): Promise<Unit> {
		activeLibraryIdState.value = libraryId

		togglePlaying(false)

		val promisedViewUpdate = updateViewFromRepository(libraryId)

		val promisedTogglePlayingUpdate = playbackService.promiseIsMarkedForPlay(libraryId).then(::togglePlaying)

		return Promise.whenAll(promisedViewUpdate, promisedTogglePlayingUpdate).unitResponse()
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
			PromiseDelay
				.delay<Any?>(screenControlVisibilityTime)
				.also(cp::doCancel)
				.then(
					{
						if (!cp.isCancelled)
							isScreenControlsVisibleState.value = false
					},
					{
						// ignored - handle to avoid excessive logging
					}
				)
		}
	}

	private fun updateViewFromRepository(libraryId: LibraryId): Promise<Unit> {
		promisedConnectionChanged.cancel()
		return nowPlayingRepository.promiseNowPlaying(libraryId)
			.eventually { np ->
				nowPlayingFileState.value = np?.playingFile
				np?.run {
					playingFile?.let { positionedFile ->
						logger.debug("File {} is playing.", positionedFile)

						provideUrlKey
							.promiseGuaranteedUrlKey(np.libraryId, positionedFile.serviceFile)
							.eventually { key ->
								val filePosition =
									if (cachedPromises?.key == key) filePositionState.value
									else filePosition

								setView(np.libraryId, positionedFile.serviceFile, filePosition)
							}
							.then(forward()) { e ->
								setTrackProgress(0)
								handleException(e)
							}
					} ?: run {
						logger.debug("No file is playing")
						artistState.value = ""
						titleState.value = stringResources.nothingPlaying
						filePositionState.value = filePosition.toInt()
						songRatingState.value = 0f
						isSongRatingEnabledState.value = false
						Unit.toPromise()
					}
				}.keepPromise(Unit)
			}
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

		val libraryId = activeLibraryId.value ?: return

		synchronized(cachedPromiseSync) {
			cachedPromises?.close()
			cachedPromises = null
		}

		pollConnections
			.pollConnection(libraryId)
			.then(
				{
					updateViewFromRepository(libraryId)
				},
				{
					promisedConnectionChanged = CancellableProxyPromise { cp ->
						applicationMessages
							.promiseReceivedMessage<LibraryConnectionChangedMessage> { m -> m.libraryId == libraryId }
							.also(cp::doCancel)
							.eventually { m -> updateViewFromRepository(m.libraryId) }
					}
				})
	}

	private fun handleIoException(exception: Throwable) =
		if (exception is UrlKeyNotReturnedException || ConnectionLostExceptionFilter.isConnectionLostException(exception)) true
		else {
			unexpectedErrorState.value = exception
			false
		}

	private fun setView(libraryId: LibraryId, serviceFile: ServiceFile, initialFilePosition: Number): Promise<Unit> {
		return provideUrlKey
			.promiseGuaranteedUrlKey(libraryId, serviceFile)
			.eventually { key ->
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
					.then { showNowPlayingControls() }
			}
	}

	private fun handleFilePropertyUpdates(message: FilePropertiesUpdatedMessage) {
		val promises = cachedPromises ?: return

		val key = message.urlServiceKey
		if (promises.key == message.urlServiceKey) {
			updateFileProperties(key, promises.libraryId, promises.serviceFile)
		}
	}

	private fun updateFileProperties(key: UrlKeyHolder<ServiceFile>, libraryId: LibraryId, serviceFile: ServiceFile) {
		val currentCachedPromises = synchronized(cachedPromiseSync) {
			cachedPromises?.close()
			CachedPromises(
				key,
				libraryId,
				serviceFile,
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

	private fun setFileProperties(fileProperties: Map<String, String>, isReadOnly: Boolean) {
		artistState.value = fileProperties[KnownFileProperties.Artist] ?: stringResources.defaultNowPlayingArtist
		titleState.value = fileProperties[KnownFileProperties.Name] ?: stringResources.defaultNowPlayingTrackTitle

		val duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties)
		setTrackDuration(if (duration > 0) duration else Int.MAX_VALUE)

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
	) : AutoCloseable
	{
		override fun close() {
			promisedIsReadOnly.cancel()
			promisedProperties.cancel()
		}
	}
}
