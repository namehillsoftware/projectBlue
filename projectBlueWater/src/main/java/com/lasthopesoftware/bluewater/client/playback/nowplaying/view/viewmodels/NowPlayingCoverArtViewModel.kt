package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.connection.polling.PollForLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.images.bytes.GetImageBytes
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.observables.LiftedInteractionState
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.observables.toSingleObservable
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.CancellationException

private val logger by lazyLogger<NowPlayingCoverArtViewModel>()

class NowPlayingCoverArtViewModel(
	applicationMessage: RegisterForApplicationMessages,
	private val nowPlayingRepository: GetNowPlayingState,
	private val provideUrlKey: ProvideUrlKey,
	private val defaultImageProvider: ProvideDefaultImage,
	private val imageProvider: GetImageBytes,
	private val pollConnections: PollForLibraryConnections,
) : ViewModel() {

	private val trackChangedSubscription = applicationMessage.registerReceiver { m: LibraryPlaybackMessage.TrackChanged ->
		setViewIfLibraryIsCorrect(m.libraryId)
	}

	private val playlistChangedSubscription = applicationMessage.registerReceiver { m: LibraryPlaybackMessage.PlaylistChanged ->
		setViewIfLibraryIsCorrect(m.libraryId)
	}

	private val isNowPlayingImageLoadingState = MutableInteractionState(false)
	private val nowPlayingImageState = MutableInteractionState(emptyByteArray)

	private val promisedDefaultImage by lazy { defaultImageProvider.promiseImageBytes() }

	private var activeLibraryId: LibraryId? = null
	private var cachedPromises: CachedPromises? = null

	val isNowPlayingImageLoading = isNowPlayingImageLoadingState.asInteractionState()
	val nowPlayingImage = LiftedInteractionState(
		promisedDefaultImage
			.toSingleObservable()
			.toObservable()
			.concatWith(nowPlayingImageState.filter { it.value.isNotEmpty() }.map { it.value }),
		emptyByteArray)

	override fun onCleared() {
		cachedPromises?.close()
		nowPlayingImage.close()
		trackChangedSubscription.close()
		playlistChangedSubscription.close()
	}

	fun initializeViewModel(libraryId: LibraryId): Promise<Unit> {
		activeLibraryId = libraryId
		return setView(libraryId)
	}

	private fun setViewIfLibraryIsCorrect(libraryId: LibraryId) {
		if (activeLibraryId == libraryId)
			setView(libraryId)
	}

	private fun setView(libraryId: LibraryId): Promise<Unit> {
		val promisedSetView = promisedDefaultImage
			.eventually { defaultImage ->
				nowPlayingRepository
					.promiseNowPlaying(libraryId)
					.eventually { np ->
						np?.playingFile?.run { setView(np.libraryId, serviceFile) } ?: run {
							nowPlayingImageState.value = defaultImage
							Unit.toPromise()
						}
					}
			}

		promisedSetView.excuse { error -> logger.warn("An error occurred initializing `NowPlayingCoverArtViewModel`", error) }

		return promisedSetView
	}

	private fun setView(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Unit> {

		fun handleException(exception: Throwable) {
			val isIoException = handleIoException(exception)
			if (!isIoException) return

			pollConnections.pollConnection(libraryId).then { _ ->
				cachedPromises?.close()
				cachedPromises = null
				activeLibraryId?.apply(::setView)
			}
		}

		fun setNowPlayingImage(cachedPromises: CachedPromises) {
			isNowPlayingImageLoadingState.value = true

			promisedDefaultImage
				.eventually { default ->
					nowPlayingImageState.value = default
					cachedPromises
						.promisedImage
						.then { bitmap ->
							if (this.cachedPromises?.urlKeyHolder == cachedPromises.urlKeyHolder) {
								nowPlayingImageState.value = bitmap.takeIf { it.isNotEmpty() } ?: default
								isNowPlayingImageLoadingState.value = false
							}
						}
				}
				.excuse { e ->
					if (e is CancellationException)	logger.debug("Bitmap retrieval cancelled", e)
					else {
						logger.error("There was an error retrieving the image for serviceFile $serviceFile", e)
						handleException(e)
					}
				}
		}

		return provideUrlKey
			.promiseGuaranteedUrlKey(libraryId, serviceFile)
			.then { urlKeyHolder ->
				if (cachedPromises?.urlKeyHolder == urlKeyHolder) return@then

				cachedPromises?.close()

				val currentCachedPromises = CachedPromises(
					urlKeyHolder,
					imageProvider.promiseImageBytes(libraryId, serviceFile)
				).also { cachedPromises = it }
				setNowPlayingImage(currentCachedPromises)
			}
	}

	private fun handleIoException(exception: Throwable) =
		ConnectionLostExceptionFilter.isConnectionLostException(exception)

	private class CachedPromises(
        val urlKeyHolder: UrlKeyHolder<ServiceFile>,
        val promisedImage: Promise<ByteArray>
	)
		: AutoCloseable
	{
		override fun close() {
			promisedImage.cancel()
		}
	}
}
