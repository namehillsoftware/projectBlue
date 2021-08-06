package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory

class FilePropertiesStorage(
	private val libraryConnections: ProvideLibraryConnections,
	private val checkRevisions: CheckRevisions,
	private val filePropertiesContainerRepository: IFilePropertiesContainerRepository
) {

	fun promiseFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		libraryConnections.promiseLibraryConnection(libraryId).eventually { connectionProvider ->
			connectionProvider?.urlProvider?.let { urlProvider ->
				if (urlProvider.authCode == null) {
					return@eventually Promise(SecurityException("Authentication is required to save properties"))
				}

				val promisedUpdate = connectionProvider.promiseResponse(
					"File/SetInfo",
					"File=${serviceFile.key}",
					"Field=$property",
					"Value=$value",
					"formatted=" + if (isFormatted) "1" else "0"
				).let { promisedServerResponse ->
					if (!logger.isInfoEnabled) {
						promisedServerResponse.unitResponse()
					} else {
						promisedServerResponse.then { response ->
							response.use {
								logger.info("api/v1/File/SetInfo responded with a response code of ${it.code}")
							}
						}
					}
				}

				urlProvider.baseUrl?.also { baseUrl ->
					promisedUpdate.eventually { checkRevisions.promiseRevision(libraryId) }
						.then { revision ->
							val urlKeyHolder = UrlKeyHolder(baseUrl, serviceFile)
							filePropertiesContainerRepository.getFilePropertiesContainer(urlKeyHolder)
								?.takeIf { it.revision == revision }
								?.updateProperty(property, value)
						}
						.excuse { e ->
							logger.warn(
								"${serviceFile.key}'s property cache item $property was not updated with the new value of $value",
								e
							)
						}
				}
				promisedUpdate
			}
		} ?: Unit.toPromise()

	companion object {
		private val logger = LoggerFactory.getLogger(FilePropertiesStorage::class.java)
	}
}
