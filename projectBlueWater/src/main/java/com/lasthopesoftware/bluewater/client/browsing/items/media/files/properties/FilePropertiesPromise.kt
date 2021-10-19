package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import okhttp3.Response
import xmlwise.Xmlwise
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

internal class FilePropertiesPromise(
	connectionProvider: IConnectionProvider,
	filePropertiesContainerProvider: IFilePropertiesContainerRepository,
	serviceFile: ServiceFile,
	serverRevision: Int
) : Promise<Map<String, String>>(), ImmediateResponse<Throwable, Unit> {

	init {
		val cancellationProxy = CancellationProxy()
		respondToCancellation(cancellationProxy)
		val filePropertiesResponse = connectionProvider.promiseResponse("File/GetInfo", "File=" + serviceFile.key)
		val promisedProperties = filePropertiesResponse
			.eventually(
				FilePropertiesWriter(
					connectionProvider,
					filePropertiesContainerProvider,
					serviceFile,
					serverRevision
				)
			)

		// Handle cancellation errors directly in stack so that they don't become unhandled
		promisedProperties.then(::resolve, this)

		cancellationProxy.doCancel(promisedProperties)
		cancellationProxy.doCancel(filePropertiesResponse)
	}

	private class FilePropertiesWriter(
		private val connectionProvider: IConnectionProvider,
		private val filePropertiesContainerProvider: IFilePropertiesContainerRepository,
		private val serviceFile: ServiceFile,
		private val serverRevision: Int
	) : PromisedResponse<Response, Map<String, String>>, CancellableMessageWriter<Map<String, String>> {
		private var response: Response? = null

		override fun promiseResponse(response: Response?): Promise<Map<String, String>> {
			this.response = response
			return QueuedPromise(this, ThreadPools.compute)
		}

		override fun prepareMessage(cancellationToken: CancellationToken): Map<String, String> =
			if (cancellationToken.isCancelled) emptyMap()
			else connectionProvider.urlProvider.baseUrl?.let { baseUrl ->
				response?.body
					?.use { body -> Xmlwise.createXml(body.string()) }
					?.let { xml ->
						val parent = xml[0]
						val returnProperties =
							parent.associateTo(HashMap(), { el -> Pair(el.getAttribute("Name"), el.value) })
						filePropertiesContainerProvider.putFilePropertiesContainer(
							UrlKeyHolder(baseUrl, serviceFile),
							FilePropertiesContainer(serverRevision, returnProperties)
						)
						returnProperties
				}
			} ?: emptyMap()
	}

	override fun respond(resolution: Throwable) {
		when (resolution) {
			is IOException -> {
				val message = resolution.message
				if (message != null && message.lowercase(Locale.getDefault()).contains("canceled")) resolve(emptyMap())
				else reject(resolution)
			}
			else -> reject(resolution)
		}
	}
}
