package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.polling.PollForLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CancellationException

private val logger by lazyLogger<NowPlayingCoverArtViewModel>()

class NowPlayingCoverArtViewModel(
	applicationMessage: RegisterForApplicationMessages,
	private val nowPlayingRepository: GetNowPlayingState,
	private val selectedConnectionProvider: ProvideSelectedConnection,
	private val defaultImageProvider: ProvideDefaultImage,
	private val imageProvider: ProvideImages,
	private val pollConnections: PollForLibraryConnections,
) : ViewModel() {

	private val trackChangedSubscription = applicationMessage.registerReceiver { m: LibraryPlaybackMessage.TrackChanged ->
		setViewIfLibraryIsCorrect(m.libraryId)
	}

	private val playlistChangedSubscription = applicationMessage.registerReceiver { m: LibraryPlaybackMessage.PlaylistChanged ->
		setViewIfLibraryIsCorrect(m.libraryId)
	}

	private val promisedDefaultImage by lazy { defaultImageProvider.promiseFileBitmap() }
	private val isNowPlayingImageLoadingState = MutableStateFlow(false)
	private val defaultImageState = MutableStateFlow<Bitmap?>(null)
	private val nowPlayingImageState = MutableStateFlow<Bitmap?>(null)
	private val unexpectedErrorState = MutableStateFlow<Throwable?>(null)

	private var activeLibraryId: LibraryId? = null
	private var cachedPromises: CachedPromises? = null

	val isNowPlayingImageLoading = isNowPlayingImageLoadingState.asStateFlow()
	val nowPlayingImage = nowPlayingImageState.asStateFlow()
	val defaultImage = defaultImageState.asStateFlow()
	val unexpectedError = unexpectedErrorState.asStateFlow()

	override fun onCleared() {
		cachedPromises?.close()
		trackChangedSubscription.close()
		playlistChangedSubscription.close()
	}

	fun initializeViewModel(libraryId: LibraryId): Promise<Unit> {
		activeLibraryId = libraryId
		return Promise.whenAll(
			setView(libraryId),
			promisedDefaultImage.then { defaultImageState.value = it }
		).unitResponse()
	}

	private fun setViewIfLibraryIsCorrect(libraryId: LibraryId) {
		if (activeLibraryId == libraryId)
			setView(libraryId)
	}

	private fun setView(libraryId: LibraryId): Promise<Unit> {
		val promisedSetView = nowPlayingRepository
			.promiseNowPlaying(libraryId)
			.eventually { np ->
				np?.playingFile?.run { setView(np.libraryId, serviceFile) }.keepPromise(Unit)
			}

		promisedSetView.excuse { error -> logger.warn("An error occurred initializing `NowPlayingActivity`", error) }

		return promisedSetView
	}

	private fun setView(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Unit> {

		fun handleException(exception: Throwable) {
			val isIoException = handleIoException(exception)
			if (!isIoException) return

			unexpectedErrorState.value = exception
			pollConnections.pollConnection(libraryId).then {
				cachedPromises?.close()
				cachedPromises = null
				activeLibraryId?.apply(::setView)
			}
		}

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
					if (e is CancellationException)	logger.debug("Bitmap retrieval cancelled", e)
					else {
						logger.error("There was an error retrieving the image for serviceFile $serviceFile", e)
						handleException(e)
					}
				}
		}

		return selectedConnectionProvider.promiseSessionConnection()
			.then { connectionProvider ->
				val baseUrl = connectionProvider?.urlProvider?.baseUrl ?: return@then

				val urlKeyHolder = UrlKeyHolder(baseUrl, serviceFile)
				if (cachedPromises?.urlKeyHolder == urlKeyHolder) return@then

				cachedPromises?.close()

				val currentCachedPromises = CachedPromises(
					urlKeyHolder,
					imageProvider.promiseFileBitmap(serviceFile)
				).also { cachedPromises = it }
				setNowPlayingImage(currentCachedPromises)
			}
	}

	private fun handleIoException(exception: Throwable) =
		if (ConnectionLostExceptionFilter.isConnectionLostException(exception)) true
		else {
			unexpectedErrorState.value = exception
			false
		}

	private class CachedPromises(
		val urlKeyHolder: UrlKeyHolder<ServiceFile>,
		val promisedImage: Promise<Bitmap?>
	)
		: AutoCloseable
	{
		override fun close() {
			promisedImage.cancel()
		}
	}
}
