package com.lasthopesoftware.bluewater.client.browsing.files.menu

import android.os.Handler
import android.widget.TextView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.*
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay.Companion.delay
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.response.EventualAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.SocketException
import java.util.*
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLProtocolException

class FileNameTextViewSetter(private val fileTextView: TextView, private val artistTextView: TextView? = null) {

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(cls<FileNameTextViewSetter>()) }
		private val timeoutDuration = Duration.standardMinutes(1)

		private val rateLimiter by lazy { PromisingRateLimiter<Map<String, String>>(1) }
	}

	private val updateSync = Any()
	private val handler = Handler(fileTextView.context.mainLooper)
	private val filePropertiesProvider by lazy {
		val context = fileTextView.context
		val libraryConnections = context.buildNewConnectionSessionManager()
		SelectedLibraryFilePropertiesProvider(
			context.getCachedSelectedLibraryIdProvider(),
			CachedFilePropertiesProvider(
				libraryConnections,
				FilePropertyCache,
				RateControlledFilePropertiesProvider(
					FilePropertiesProvider(
						libraryConnections,
						LibraryRevisionProvider(libraryConnections),
						FilePropertyCache,
					),
					rateLimiter,
				)
			),
		)
	}

	@Volatile
	private var promisedState = Unit.toPromise()

	@Volatile
	private var currentlyPromisedViewUpdate: PromisedTextViewUpdate? = null

	@Synchronized
	fun promiseTextViewUpdate(serviceFile: ServiceFile): Promise<Unit> {
		val currentPromisedState = promisedState
		promisedState = currentPromisedState.inevitably(EventualTextViewUpdate(serviceFile))
		currentPromisedState.cancel()
		return promisedState
	}

	private inner class EventualTextViewUpdate(private val serviceFile: ServiceFile) : EventualAction {
		override fun promiseAction(): Promise<*> =
			synchronized(updateSync) {
				PromisedTextViewUpdate(serviceFile)
					.apply {
						currentlyPromisedViewUpdate = this
						beginUpdate()
					}
			}
	}

	private inner class PromisedTextViewUpdate(private val serviceFile: ServiceFile) :
		Promise<Unit>(), Runnable, ImmediateResponse<Map<String, String>, Unit> {

		private val cancellationProxy = CancellationProxy()

		init {
			respondToCancellation(cancellationProxy)
		}

		fun beginUpdate() {
			if (handler.looper.thread === Thread.currentThread()) {
				run()
				return
			}

			if (handler.post(this)) return

			logger.warn("Handler failed to post text view update: $handler")
			resolve(Unit)
		}

		override fun run() {
			fileTextView.setText(R.string.lbl_loading)
			artistTextView?.text = ""

			if (isNotCurrentPromise || isUpdateCancelled) return resolve(Unit)

			val filePropertiesPromise = filePropertiesProvider.promiseFileProperties(serviceFile)
			val promisedViewSetting = filePropertiesPromise.eventually(response(this, handler))

			val delayPromise = delay<Unit>(timeoutDuration)
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

			cancellationProxy.doCancel(filePropertiesPromise)
			cancellationProxy.doCancel(delayPromise)
		}

		override fun respond(properties: Map<String, String>) {
			if (isNotCurrentPromise || isUpdateCancelled) return

			val trackName = properties[KnownFileProperties.NAME]
			if (trackName != null) fileTextView.text = trackName

			artistTextView?.text = properties[KnownFileProperties.ARTIST] ?: "Unknown Artist"
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

		private val isNotCurrentPromise: Boolean
			get() = synchronized(updateSync) { currentlyPromisedViewUpdate !== this }
		private val isUpdateCancelled: Boolean
			get() = cancellationProxy.isCancelled
	}
}
