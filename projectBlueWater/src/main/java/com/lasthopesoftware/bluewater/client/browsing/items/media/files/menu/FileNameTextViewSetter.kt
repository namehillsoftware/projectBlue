package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu

import android.os.Handler
import android.widget.TextView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SelectedConnectionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionProvider
import com.lasthopesoftware.bluewater.shared.exceptions.LoggerUncaughtExceptionHandler
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay.Companion.delay
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response
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

class FileNameTextViewSetter(private val textView: TextView) {

	companion object {
		private val logger = LoggerFactory.getLogger(FileNameTextViewSetter::class.java)
		private val timeoutDuration = Duration.standardMinutes(1)
	}

	private val textViewUpdateSync = Any()
	private val handler = Handler(textView.context.mainLooper)
	private val lazyFilePropertiesProvider = lazy {
		SelectedConnectionFilePropertiesProvider(SelectedConnectionProvider(textView.context)) { c ->
			val filePropertyCache = FilePropertyCache.getInstance()
			ScopedCachedFilePropertiesProvider(
				c,
				filePropertyCache,
				ScopedFilePropertiesProvider(
					c,
					ScopedRevisionProvider(c),
					filePropertyCache
				)
			)
		}
	}

	@Volatile
	private var promisedState = Promise.empty<Void>()

	@Volatile
	private var currentlyPromisedTextViewUpdate: PromisedTextViewUpdate? = null

	@Synchronized
	fun promiseTextViewUpdate(serviceFile: ServiceFile): Promise<Void> {
		promisedState.cancel()
		promisedState = promisedState
			.inevitably(EventualTextViewUpdate(serviceFile))
		return promisedState
	}

	private inner class EventualTextViewUpdate(private val serviceFile: ServiceFile) : EventualAction {
		override fun promiseAction(): Promise<*> =
			synchronized(textViewUpdateSync) {
				PromisedTextViewUpdate(serviceFile)
					.apply {
						currentlyPromisedTextViewUpdate = this
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
			textView.setText(R.string.lbl_loading)

			if (isNotCurrentPromise || isUpdateCancelled) return resolve(Unit)

			val filePropertiesPromise = lazyFilePropertiesProvider.value.promiseFileProperties(serviceFile)
			cancellationProxy.doCancel(filePropertiesPromise)

			val promisedViewSetting = filePropertiesPromise.eventually(response(this, handler))

			val delayPromise = delay<Unit>(timeoutDuration)
			cancellationProxy.doCancel(delayPromise)
			whenAny(promisedViewSetting, delayPromise)
				.must {

					// First, cancel everything if the delay promise finished first
					delayPromise.then { cancellationProxy.run() }
					// Then cancel the delay promise, in case the promised view setting
					// finished first
					delayPromise.cancel()

					// Finally, always resolve the parent promise
					resolve(Unit)
				}
				.excuse { e ->
					LoggerUncaughtExceptionHandler
						.getErrorExecutor()
						.execute { handleError(e) }
				}
		}

		override fun respond(properties: Map<String, String>) {
			if (isNotCurrentPromise || isUpdateCancelled) return
			val fileName = properties[KnownFileProperties.NAME]
			if (fileName != null) textView.text = fileName
		}

		private fun handleError(e: Throwable) {
			if (isUpdateCancelled) return
			if (e is CancellationException) return
			if (e is SocketException) {
				val message = e.message
				if (message != null && message.lowercase(Locale.getDefault()).contains("socket closed")) return
			}
			if (e is IOException) {
				val message = e.message
				if (message != null && message.lowercase(Locale.getDefault()).contains("canceled")) return
			}
			if (e is SSLProtocolException) {
				val message = e.message
				if (message != null && message.lowercase(Locale.getDefault()).contains("ssl handshake aborted")) return
			}
			logger.error(
				"An error occurred getting the file properties for the file with ID " + serviceFile.key,
				e
			)
		}

		private val isNotCurrentPromise: Boolean
			get() = synchronized(textViewUpdateSync) { currentlyPromisedTextViewUpdate !== this }
		private val isUpdateCancelled: Boolean
			get() = cancellationProxy.isCancelled
	}
}
