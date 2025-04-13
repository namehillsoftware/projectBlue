package com.lasthopesoftware.bluewater.client.browsing.files.list

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.exceptions.isOkHttpCanceled
import com.lasthopesoftware.exceptions.isSocketClosedException
import com.lasthopesoftware.promises.PromiseDelay
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.unitResponse
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
import java.util.Locale
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLProtocolException

private val timeoutDuration by lazy { Duration.standardMinutes(1) }
private val logger by lazyLogger<ReusableFileViewModel>()

class ReusableFileViewModel(
	private val filePropertiesProvider: ProvideLibraryFileProperties,
	private val stringResources: GetStringResources,
	private val urlKeyProvider: ProvideUrlKey,
	receiveMessages: RegisterForApplicationMessages,
) : ViewFileItem,  (FilePropertiesUpdatedMessage) -> Unit {

	private val filePropertiesUpdatedSubscription = receiveMessages.registerReceiver(this)

	private val promiseSync = Any()

	@Volatile
	private var activeUrlKey: UrlKeyHolder<ServiceFile>? = null

	@Volatile
	private var activeLibraryId: LibraryId? = null

	@Volatile
	private var activeServiceFile: ServiceFile? = null

	@Volatile
	private var promisedState = Unit.toPromise()

	private val mutableArtist = MutableStateFlow("")
	private val mutableTitle = MutableStateFlow(stringResources.loading)

	override val artist = mutableArtist.asStateFlow()
	override val title = mutableTitle.asStateFlow()

	override fun promiseUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Unit> =
		synchronized(promiseSync) {
			activeLibraryId = libraryId
			activeServiceFile = serviceFile

			val currentPromisedState = promisedState
			promisedState = currentPromisedState.inevitably(EventualTextViewUpdate(libraryId, serviceFile))
			currentPromisedState.cancel()
			promisedState
		}

	override fun invoke(message: FilePropertiesUpdatedMessage) {
		if (activeUrlKey == message.urlServiceKey) activeServiceFile?.also { f ->
			activeLibraryId?.also { l ->
				promiseUpdate(l, f)
			}
		}
	}

	override fun close() {
		reset()
		filePropertiesUpdatedSubscription.close()
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
	}

	private inner class EventualTextViewUpdate(private val libraryId: LibraryId, private val serviceFile: ServiceFile) : EventualAction {
		override fun promiseAction(): Promise<*> = PromisedTextViewUpdate(libraryId, serviceFile)
	}

	private inner class PromisedTextViewUpdate(private val libraryId: LibraryId, private val serviceFile: ServiceFile) :
		Promise<Unit>(), ImmediateResponse<Map<String, String>, Unit> {

		private val cancellationProxy = CancellationProxy()

		init {
			awaitCancellation(cancellationProxy)

			beginUpdate()
		}

		fun beginUpdate() {
			resetState()

			if (isNotCurrentServiceFile || isUpdateCancelled) return resolve(Unit)

			val filePropertiesPromise = filePropertiesProvider.promiseFileProperties(libraryId, serviceFile)

			val promisedUrlKey = urlKeyProvider.promiseUrlKey(libraryId, serviceFile)

			val promisedViewSetting = filePropertiesPromise.then(this)

			val delayPromise = PromiseDelay.delay<Unit>(timeoutDuration)
			whenAny(whenAll(promisedViewSetting, promisedUrlKey.then { it -> activeUrlKey = it }).unitResponse(), delayPromise, cancellationProxy.unitResponse())
				.must { _ ->
					// First, cancel everything to ensure the losing task doesn't continue running
					cancellationProxy.cancellationRequested()

					// Finally, always resolve the parent promise
					resolve(Unit)
				}
				.excuse { e ->
					ThreadPools
						.exceptionsLogger
						.execute { handleError(e) }
				}

			cancellationProxy.doCancel(promisedUrlKey)
			cancellationProxy.doCancel(filePropertiesPromise)
			cancellationProxy.doCancel(delayPromise)
		}

		override fun respond(properties: Map<String, String>) {
			if (isNotCurrentServiceFile || isUpdateCancelled) return

			mutableTitle.value = properties[NormalizedFileProperties.Name] ?: stringResources.unknownTrack
			mutableArtist.value = properties[NormalizedFileProperties.Artist] ?: stringResources.unknownArtist
		}

		private fun handleError(e: Throwable) {
			if (isUpdateCancelled) return

			when (e) {
				is CancellationException -> return
				is SSLProtocolException -> {
					val message = e.message
					if (message != null && message.lowercase(Locale.getDefault()).contains("ssl handshake aborted")) return
				}
				is IOException -> {
					if (e.isOkHttpCanceled() || e.isSocketClosedException()) return
				}
			}

			logger.error("An error occurred getting the file properties for the file with ID " + serviceFile.key, e)
		}

		private val isNotCurrentServiceFile: Boolean
			get() = activeServiceFile != serviceFile
		private val isUpdateCancelled: Boolean
			get() = cancellationProxy.isCancelled
	}
}
