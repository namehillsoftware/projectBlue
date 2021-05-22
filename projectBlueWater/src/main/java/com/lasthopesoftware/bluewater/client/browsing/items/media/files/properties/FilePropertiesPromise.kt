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
import com.namehillsoftware.handoff.promises.response.VoidResponse
import okhttp3.Response
import org.slf4j.LoggerFactory
import xmlwise.XmlParseException
import xmlwise.Xmlwise
import java.io.IOException
import java.util.*

internal class FilePropertiesPromise(
	connectionProvider: IConnectionProvider,
	filePropertiesContainerProvider: IFilePropertiesContainerRepository,
	serviceFile: ServiceFile,
	serverRevision: Int
) : Promise<Map<String, String>>() {

	private class FilePropertiesWriter(
		private val connectionProvider: IConnectionProvider,
		private val filePropertiesContainerProvider: IFilePropertiesContainerRepository,
		private val serviceFile: ServiceFile,
		private val serverRevision: Int
	) : PromisedResponse<Response, Map<String, String>>, CancellableMessageWriter<Map<String, String>> {
		private var response: Response? = null

		override fun prepareMessage(cancellationToken: CancellationToken): Map<String, String> {
			if (cancellationToken.isCancelled) return HashMap()
			val body = response!!.body ?: return HashMap()
			return try {
				val xml = Xmlwise.createXml(body.string())
				val parent = xml[0]
				val returnProperties = HashMap<String, String>(parent.size)
				for (el in parent) returnProperties[el.getAttribute("Name")] = el.value
				val urlKeyHolder = UrlKeyHolder(connectionProvider.urlProvider.baseUrl, serviceFile)
				filePropertiesContainerProvider.putFilePropertiesContainer(
					urlKeyHolder, FilePropertiesContainer(
						serverRevision, returnProperties
					)
				)
				returnProperties
			} catch (e: IOException) {
				LoggerFactory.getLogger(SessionFilePropertiesProvider::class.java).error(e.toString(), e)
				throw e
			} catch (e: XmlParseException) {
				LoggerFactory.getLogger(SessionFilePropertiesProvider::class.java).error(e.toString(), e)
				throw e
			} finally {
				body.close()
			}
		}

		override fun promiseResponse(response: Response?): Promise<Map<String, String>> {
			this.response = response
			return QueuedPromise(this, ParsingScheduler.instance().scheduler)
		}
	}

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
		promisedProperties.then(
			VoidResponse { resolution: Map<String, String>? -> this.resolve(resolution) },
			VoidResponse { error: Throwable? -> reject(error) })
	}
}
