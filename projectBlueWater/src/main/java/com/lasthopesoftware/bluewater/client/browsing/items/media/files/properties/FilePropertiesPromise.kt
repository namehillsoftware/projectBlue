package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.resources.scheduling.ParsingScheduler
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import okhttp3.Response
import xmlwise.Xmlwise

internal class FilePropertiesPromise(
	connectionProvider: IConnectionProvider,
	filePropertiesContainerProvider: IFilePropertiesContainerRepository,
	serviceFile: ServiceFile,
	serverRevision: Int
) : Promise<Map<String, String>>() {

	init {
		val cancellationProxy = CancellationProxy()
		respondToCancellation(cancellationProxy)
		val filePropertiesResponse = connectionProvider.promiseResponse("File/GetInfo", "File=" + serviceFile.key)
		cancellationProxy.doCancel(filePropertiesResponse)
		val promisedProperties = filePropertiesResponse
			.eventually(
				FilePropertiesWriter(
					connectionProvider,
					filePropertiesContainerProvider,
					serviceFile,
					serverRevision
				)
			)
		cancellationProxy.doCancel(promisedProperties)
		promisedProperties.then(::resolve, ::reject)
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
			return QueuedPromise(this, ParsingScheduler.instance().scheduler)
		}

		override fun prepareMessage(cancellationToken: CancellationToken): Map<String, String> =
			if (cancellationToken.isCancelled) HashMap()
			else response?.body?.use { body ->
				val xml = Xmlwise.createXml(body.string())
				val parent = xml[0]
				val returnProperties = parent.associateTo(HashMap(), { el -> Pair(el.getAttribute("Name"), el.value) })
				filePropertiesContainerProvider.putFilePropertiesContainer(
					UrlKeyHolder(connectionProvider.urlProvider.baseUrl, serviceFile),
					FilePropertiesContainer(serverRevision, returnProperties)
				)
				returnProperties
			} ?: HashMap()
	}
}
