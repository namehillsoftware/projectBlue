package com.lasthopesoftware.bluewater.client.browsing.files.list

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideScopedUrlKey
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
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
private val logger by lazyLogger<ReusableFileViewModel>()

class ReusableFileViewModel(
	private val filePropertiesProvider: ProvideScopedFileProperties,
	private val stringResources: GetStringResources,
	private val urlKeyProvider: ProvideScopedUrlKey,
	receiveMessages: RegisterForApplicationMessages,
) : ViewFileItem,  (FilePropertiesUpdatedMessage) -> Unit {

	private val filePropertiesUpdatedSubscription = receiveMessages.registerReceiver(this)

	private val promiseSync = Any()

	@Volatile
	private var activeUrlKey: UrlKeyHolder<ServiceFile>? = null

	@Volatile
	private var activeServiceFile: ServiceFile? = null

	@Volatile
	private var promisedState = Unit.toPromise()

	private val mutableArtist = MutableStateFlow("")
	private val mutableTitle = MutableStateFlow(stringResources.loading)

	override val artist = mutableArtist.asStateFlow()
	override val title = mutableTitle.asStateFlow()

	override fun promiseUpdate(serviceFile: ServiceFile): Promise<Unit> =
		synchronized(promiseSync) {
			activeServiceFile = serviceFile

			val currentPromisedState = promisedState
			promisedState = currentPromisedState.inevitably(EventualTextViewUpdate(serviceFile))
			currentPromisedState.cancel()
			promisedState
		}

	override fun invoke(message: FilePropertiesUpdatedMessage) {
		if (activeUrlKey == message.urlServiceKey) activeServiceFile?.also {
			promiseUpdate(it)
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
			val promisedUrlKey = urlKeyProvider
				.promiseUrlKey(serviceFile)
				.then { activeUrlKey = it }
			cancellationProxy.doCancel(filePropertiesPromise)

			val promisedViewSetting = filePropertiesPromise.then(this)

			val delayPromise = PromiseDelay.delay<Collection<Unit>>(timeoutDuration)
			whenAny(whenAll(promisedViewSetting, promisedUrlKey), delayPromise)
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

			mutableTitle.value = properties[KnownFileProperties.Name] ?: stringResources.unknownTrack
			mutableArtist.value = properties[KnownFileProperties.Artist] ?: stringResources.unknownArtist
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
			get() = activeServiceFile != serviceFile
		private val isUpdateCancelled: Boolean
			get() = cancellationProxy.isCancelled
	}
}