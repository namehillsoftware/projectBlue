package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideGuaranteedLibraryConnections
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.exceptions.isOkHttpCanceled
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class FilePropertiesProvider(
	private val libraryConnections: ProvideGuaranteedLibraryConnections,
	private val checkRevisions: CheckRevisions,
	private val filePropertiesContainerProvider: IFilePropertiesContainerRepository
) : ProvideFreshLibraryFileProperties {

	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>> =
		ProxiedFileProperties(libraryId, serviceFile)

	private inner class ProxiedFileProperties(private val libraryId: LibraryId, private val serviceFile: ServiceFile) :
		Promise.Proxy<Map<String, String>>(), PromisedResponse<ProvideConnections, Map<String, String>>
	{
		init {
			proxy(
				libraryConnections
					.promiseLibraryConnection(libraryId)
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(connectionProvider: ProvideConnections): Promise<Map<String, String>> =
			if (isCancelled) Promise(FilePropertiesCancellationException(libraryId, serviceFile))
			else checkRevisions
				.promiseRevision(libraryId)
				.also(::doCancel)
				.eventually { revision ->
					FilePropertiesPromise(
						connectionProvider,
						libraryId,
						serviceFile,
						revision
					)
				}
	}

	private inner class FilePropertiesPromise(
		private val connectionProvider: ProvideConnections,
		private val libraryId: LibraryId,
		private val serviceFile: ServiceFile,
		private val serverRevision: Int
	) :
		Promise<Map<String, String>>(),
		PromisedResponse<Response, Unit>,
		CancellableMessageWriter<Unit>,
		ImmediateResponse<Throwable, Unit>
	{
		private val cancellationProxy = CancellationProxy()
		private lateinit var response: Response

		private val urlKeyHolder
			get() = connectionProvider.urlProvider.baseUrl.let { UrlKeyHolder(it, serviceFile) }

		init {
			val fileProperties = urlKeyHolder
				.let(filePropertiesContainerProvider::getFilePropertiesContainer)
				?.takeIf { it.properties.isNotEmpty() && serverRevision == it.revision }
				?.properties

			if (fileProperties != null) resolve(fileProperties)
			else {
				awaitCancellation(cancellationProxy)
				val filePropertiesResponse = connectionProvider.promiseResponse("File/GetInfo", "File=" + serviceFile.key)
				val promisedProperties = filePropertiesResponse.eventually(this)

				// Handle cancellation errors directly in stack so that they don't become unhandled
				promisedProperties.excuse(this)

				cancellationProxy.doCancel(filePropertiesResponse)
			}
		}

		override fun promiseResponse(resolution: Response): Promise<Unit> {
			response = resolution
			return QueuedPromise(this, ThreadPools.compute)
		}

		override fun prepareMessage(cancellationSignal: CancellationSignal) {
			if (cancellationProxy.isCancelled) {
				response.close()
				reject(FilePropertiesCancellationException(libraryId, serviceFile))
				return
			}

			resolve(
				response.body
					.use { body -> body.string() }
					.let(Jsoup::parse)
					.let { xml ->
						xml
							.getElementsByTag("item")
							.firstOrNull()
							?.children()
							?.associateTo(HashMap()) { el ->Pair(el.attr("Name"), el.wholeOwnText()) }
							?: emptyMap()
					}
					.also { properties ->
						filePropertiesContainerProvider.putFilePropertiesContainer(
							urlKeyHolder,
							FilePropertiesContainer(serverRevision, properties)
						)
					}
			)
		}

		override fun respond(rejection: Throwable) {
			when (rejection) {
				is IOException -> {
					if (cancellationProxy.isCancelled && rejection.isOkHttpCanceled()) {
						reject(FilePropertiesCancellationException(libraryId, serviceFile))
					} else {
						reject(rejection)
					}
				}
				else -> reject(rejection)
			}
		}
	}

	private class FilePropertiesCancellationException(libraryId: LibraryId, serviceFile: ServiceFile)
		: CancellationException("Getting file properties cancelled for $libraryId and $serviceFile.")
}
