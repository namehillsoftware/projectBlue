package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import okhttp3.Response
import xmlwise.Xmlwise
import java.io.IOException
import java.util.*

internal class FilePropertiesPromise(
	private val connectionProvider: IConnectionProvider,
	private val filePropertiesContainerProvider: IFilePropertiesContainerRepository,
	private val serviceFile: ServiceFile,
	private val serverRevision: Int
) :
	Promise<Map<String, String>>(),
	PromisedResponse<Response, Unit>,
	MessageWriter<Unit>,
	ImmediateResponse<Throwable, Unit>
{
	private val cancellationProxy = CancellationProxy()
	private lateinit var response: Response

	private val urlKeyHolder
		get() = connectionProvider.urlProvider.baseUrl?.let { UrlKeyHolder(it, serviceFile) }

	init {
		val fileProperties = urlKeyHolder
			?.let(filePropertiesContainerProvider::getFilePropertiesContainer)
			?.takeIf { it.properties.isNotEmpty() && serverRevision == it.revision }
			?.properties

		if (fileProperties != null) resolve(fileProperties)
		else {
			respondToCancellation(cancellationProxy)
			val filePropertiesResponse = connectionProvider.promiseResponse("File/GetInfo", "File=" + serviceFile.key)
			val promisedProperties = filePropertiesResponse.eventually(this)

			// Handle cancellation errors directly in stack so that they don't become unhandled
			promisedProperties.excuse(this)

			cancellationProxy.doCancel(promisedProperties)
			cancellationProxy.doCancel(filePropertiesResponse)
		}
	}

	override fun promiseResponse(resolution: Response): Promise<Unit> {
		response = resolution
		return QueuedPromise(this, ThreadPools.compute)
	}

	override fun prepareMessage() {
		val result = if (cancellationProxy.isCancelled) emptyMap()
		else urlKeyHolder?.let { key ->
			response.body
				?.use { body -> Xmlwise.createXml(body.string()) }
				?.let { xml ->
					val parent = xml[0]
					parent.associateTo(HashMap()) { el -> Pair(el.getAttribute("Name"), el.value) }
				}
				?.also { properties ->
					filePropertiesContainerProvider.putFilePropertiesContainer(
						key,
						FilePropertiesContainer(serverRevision, properties)
					)
				}
		} ?: emptyMap()

		resolve(result)
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
