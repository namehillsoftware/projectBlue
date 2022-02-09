package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.viewmodels

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ProvideImages
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.polling.PollForConnections
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.CancellationException

private val logger by lazy { LoggerFactory.getLogger(NowPlayingCoverArtViewModel::class.java) }

class NowPlayingCoverArtViewModel(
	private val messages: RegisterForMessages,
	private val nowPlayingRepository: INowPlayingRepository,
	private val selectedConnectionProvider: ProvideSelectedConnection,
	private val defaultImageProvider: ProvideDefaultImage,
	private val imageProvider: ProvideImages,
	private val pollConnections: PollForConnections,
) : ViewModel(), Closeable {
	private val onPlaybackChangedReceiver: BroadcastReceiver

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
		onPlaybackChangedReceiver = object : BroadcastReceiver() {
			override fun onReceive(context: Context?, intent: Intent?) {
				setView()
			}
		}

		val playingFileChangedFilter = IntentFilter().apply {
			addAction(PlaylistEvents.onPlaylistTrackChange)
			addAction(PlaylistEvents.onPlaylistChange)
		}

		messages.registerReceiver(onPlaybackChangedReceiver, playingFileChangedFilter)
	}

	override fun close() {
		cachedPromises?.close()
		messages.unregisterReceiver(onPlaybackChangedReceiver)
	}

	fun initializeViewModel() {
		setView()

		promisedDefaultImage.then { defaultImageState.value = it }
	}

	private fun setView() {
		nowPlayingRepository.nowPlaying
			.then { np ->
				np.playingFile?.also { positionedFile -> setView(positionedFile.serviceFile) }
			}
			.excuse { error -> logger.warn("An error occurred initializing `NowPlayingActivity`", error) }
	}

	private fun handleIoException(exception: Throwable) =
		if (ConnectionLostExceptionFilter.isConnectionLostException(exception)) true
		else {
			unexpectedErrorState.value = exception
			false
		}

	private fun setView(serviceFile: ServiceFile) {

		fun handleException(exception: Throwable) {
			val isIoException = handleIoException(exception)
			if (!isIoException) return

			unexpectedErrorState.value = exception
			pollConnections.pollSessionConnection().then {
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

		selectedConnectionProvider.promiseSessionConnection()
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
