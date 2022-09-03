package com.lasthopesoftware.bluewater.client.browsing.files.list

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.LaunchFileDetails
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.HiddenListItemMenu
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.response.EventualAction
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
private val logger by lazyLogger<ReusableTrackHeadlineViewModel>()

class ReusableTrackHeadlineViewModel(
	private val filePropertiesProvider: ProvideScopedFileProperties,
	private val stringResources: GetStringResources,
	private val controlPlaybackService: ControlPlaybackService,
	private val fileDetailsLauncher: LaunchFileDetails,
	private val sendItemMenuMessages: SendTypedMessages<ItemListMenuMessage>,
) : ViewFileItem, HiddenListItemMenu {

	private val promiseSync = Any()

	@Volatile
	private var promisedState = Unit.toPromise()

	@Volatile
	private var activePositionedFile: PositionedFile? = null

	@Volatile
	private var associatedPlaylist = emptyList<ServiceFile>()

	private val mutableArtist = MutableStateFlow("")
	private val mutableTitle = MutableStateFlow(stringResources.loading)
	private val mutableIsMenuShown = MutableStateFlow(false)

	override val artist = mutableArtist.asStateFlow()
	override val title = mutableTitle.asStateFlow()
	override val isMenuShown = mutableIsMenuShown.asStateFlow()

	override fun promiseUpdate(playlist: List<ServiceFile>, position: Int): Promise<Unit> {
		synchronized(promiseSync) {
			val serviceFile = playlist[position]
			activePositionedFile = PositionedFile(position, serviceFile)
			associatedPlaylist = playlist

			val currentPromisedState = promisedState
			promisedState = currentPromisedState.inevitably(EventualTextViewUpdate(serviceFile))
			currentPromisedState.cancel()
			return promisedState
		}
	}

	override fun showMenu(): Boolean =
		mutableIsMenuShown.compareAndSet(expect = false, update = true)
			.also {
				if (it) sendItemMenuMessages.sendMessage(ItemListMenuMessage.MenuShown(this))
			}

	override fun hideMenu(): Boolean =
		mutableIsMenuShown.compareAndSet(expect = true, update = false)
			.also {
				if (it) sendItemMenuMessages.sendMessage(ItemListMenuMessage.MenuHidden(this))
			}

	override fun addToNowPlaying() {
		activePositionedFile?.serviceFile?.apply(controlPlaybackService::addToPlaylist)
		hideMenu()
	}

	override fun viewFileDetails() {
		activePositionedFile?.apply {
			fileDetailsLauncher.launchFileDetails(associatedPlaylist, playlistPosition)
		}
		hideMenu()
	}

	override fun reset() {
		synchronized(promiseSync) {
			promisedState.cancel()
		}

		resetState()
	}

	private fun resetState() {
		mutableTitle.value = stringResources.loading
		mutableArtist.value = ""
		hideMenu()
	}

	private inner class EventualTextViewUpdate(private val serviceFile: ServiceFile) : EventualAction {
		override fun promiseAction(): Promise<*> = PromisedTextViewUpdate(serviceFile)
	}

	private inner class PromisedTextViewUpdate(private val serviceFile: ServiceFile) :
		Promise<Unit>(), ImmediateResponse<Map<String, String>, Unit> {

		private val cancellationProxy = CancellationProxy()

		init {
			respondToCancellation(cancellationProxy)

			beginUpdate()
		}

		fun beginUpdate() {
			resetState()

			if (isNotCurrentServiceFile || isUpdateCancelled) return resolve(Unit)

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
			if (isNotCurrentServiceFile || isUpdateCancelled) return

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

		private val isNotCurrentServiceFile: Boolean
			get() = activePositionedFile?.serviceFile != serviceFile
		private val isUpdateCancelled: Boolean
			get() = cancellationProxy.isCancelled
	}
}
