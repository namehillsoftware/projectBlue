package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.LaunchFileDetails
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.joda.time.Duration
import java.io.IOException
import java.net.SocketException
import java.util.*
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLProtocolException

private val timeoutDuration by lazy { Duration.standardMinutes(1) }
private val logger by lazyLogger<SingleUseTrackHeadlineViewModel>()

class SingleUseTrackHeadlineViewModel(
	val serviceFile: ServiceFile,
	private val filePropertiesProvider: ProvideScopedFileProperties,
	private val stringResources: GetStringResources,
	private val controlPlaybackService: ControlPlaybackService,
	private val fileDetailsLauncher: LaunchFileDetails,
) {

	private val mutableArtist = MutableStateFlow("")
	private val mutableTitle = MutableStateFlow(stringResources.loading)
	private val mutableIsMenuShown = MutableStateFlow(false)

	val artist = mutableArtist.asStateFlow()
	val title = mutableTitle.asStateFlow()
	val isMenuShown = mutableIsMenuShown.asStateFlow()

	fun close() {}

	fun promiseUpdate(): Promise<Unit> = PromisedTextViewUpdate(serviceFile)

	fun showMenu() {
		mutableIsMenuShown.value = !mutableIsMenuShown.value
	}

	fun hideMenu(): Boolean = mutableIsMenuShown.compareAndSet(expect = true, update = false)

	fun addToNowPlaying() {
		controlPlaybackService.addToPlaylist(serviceFile)
	}

	fun viewFileDetails() {
		fileDetailsLauncher.launchFileDetails(serviceFile)
	}

	private inner class PromisedTextViewUpdate(private val serviceFile: ServiceFile) :
		Promise<Unit>(), ImmediateResponse<Map<String, String>, Unit> {

		private val cancellationProxy = CancellationProxy()

		init {
			respondToCancellation(cancellationProxy)

			beginUpdate()
		}

		fun beginUpdate() {
			if (isUpdateCancelled) return resolve(Unit)

			val filePropertiesPromise = filePropertiesProvider.promiseFileProperties(serviceFile)
			cancellationProxy.doCancel(filePropertiesPromise)

			val promisedViewSetting = filePropertiesPromise.then(this)

			val delayPromise = PromiseDelay.delay<Unit>(timeoutDuration)
			whenAny(promisedViewSetting, delayPromise)
				.must {

					// First, cancel everything to ensure the losing task doesn't continue running
					cancellationProxy.run()

					// Finally, always resolve the parent promise
					resolve(Unit)
				}
				.excuse { e ->
					ThreadPools
						.exceptionsLogger
						.execute { handleError(e) }
				}

			cancellationProxy.doCancel(delayPromise)
		}

		override fun respond(properties: Map<String, String>) {
			if (isUpdateCancelled) return

			mutableTitle.value = properties[KnownFileProperties.NAME] ?: stringResources.unknownTrack
			mutableArtist.value = properties[KnownFileProperties.ARTIST] ?: stringResources.unknownArtist
		}

		private fun handleError(e: Throwable) {
			if (isUpdateCancelled) return

			when (e) {
				is CancellationException -> return
				is SocketException -> {
					val message = e.message
					if (message != null && message.lowercase(Locale.getDefault()).contains("socket closed")) return
				}
				is SSLProtocolException -> {
					val message = e.message
					if (message != null && message.lowercase(Locale.getDefault()).contains("ssl handshake aborted")) return
				}
				is IOException -> {
					val message = e.message
					if (message != null && message.lowercase(Locale.getDefault()).contains("canceled")) return
				}
			}

			logger.error(
				"An error occurred getting the file properties for the file with ID " + serviceFile.key,
				e
			)
		}

		private val isUpdateCancelled: Boolean
			get() = cancellationProxy.isCancelled
	}
}
