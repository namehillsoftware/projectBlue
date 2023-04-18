package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.polling.PollForConnections
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage.PlaylistChanged
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage.TrackChanged
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CancellationException

private val logger by lazyLogger<NowPlayingCoverArtViewModel>()

class NowPlayingCoverArtViewModel(
	private val applicationMessage: RegisterForApplicationMessages,
	private val nowPlayingRepository: GetNowPlayingState,
	private val selectedConnectionProvider: ProvideSelectedConnection,
	private val defaultImageProvider: ProvideDefaultImage,
	private val imageProvider: ProvideImages,
	private val pollConnections: PollForConnections,
) : ViewModel(), (ApplicationMessage) -> Unit {
	private var cachedPromises: CachedPromises? = null

	private val promisedDefaultImage by lazy { defaultImageProvider.promiseFileBitmap() }

	private val isNowPlayingImageLoadingState = MutableStateFlow(false)
	private val defaultImageState = MutableStateFlow<Bitmap?>(null)
	private val nowPlayingImageState = MutableStateFlow<Bitmap?>(null)
	private val unexpectedErrorState = MutableStateFlow<Throwable?>(null)

	val isNowPlayingImageLoading = isNowPlayingImageLoadingState.asStateFlow()
	val nowPlayingImage = nowPlayingImageState.asStateFlow()
	val defaultImage = defaultImageState.asStateFlow()
	val unexpectedError = unexpectedErrorState.asStateFlow()

	init {
		applicationMessage.registerForClass(cls<TrackChanged>(), this)
		applicationMessage.registerForClass(cls<PlaylistChanged>(), this)
	}

	override fun invoke(p1: ApplicationMessage) {
		setView()
	}

	override fun onCleared() {
		cachedPromises?.close()
		applicationMessage.unregisterReceiver(this)
	}

	fun initializeViewModel(): Promise<Unit> {

		return Promise.whenAll(
			setView(),
			promisedDefaultImage.then { defaultImageState.value = it }
		).unitResponse()
	}

	private fun setView(): Promise<Unit> {
		val promisedSetView = nowPlayingRepository
			.promiseNowPlaying()
			.eventually { np ->
				np?.playingFile?.run { setView(np.libraryId, serviceFile) }.keepPromise(Unit)
			}

		promisedSetView.excuse { error -> logger.warn("An error occurred initializing `NowPlayingActivity`", error) }

		return promisedSetView
	}

	private fun handleIoException(exception: Throwable) =
		if (ConnectionLostExceptionFilter.isConnectionLostException(exception)) true
		else {
			unexpectedErrorState.value = exception
			false
		}

	private fun setView(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Unit> {

		fun handleException(exception: Throwable) {
			val isIoException = handleIoException(exception)
			if (!isIoException) return

			unexpectedErrorState.value = exception
			pollConnections.pollConnection(libraryId).then {
				cachedPromises?.close()
				cachedPromises = null
				setView()
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
