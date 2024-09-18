package com.lasthopesoftware.bluewater.client.browsing.files.properties.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

private val logger by lazyLogger<FilePropertyStorage>()

class FilePropertyStorage(
	private val libraryConnections: ProvideLibraryConnections,
	private val checkIfConnectionIsReadOnly: CheckIfConnectionIsReadOnly,
	private val checkRevisions: CheckRevisions,
	private val filePropertiesContainerRepository: IFilePropertiesContainerRepository,
	private val sendApplicationMessages: SendApplicationMessages
) : UpdateFileProperties {
	override fun promiseFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		checkIfConnectionIsReadOnly
			.promiseIsReadOnly(libraryId)
			.eventually { isReadOnly ->
				if (!isReadOnly) libraryConnections
					.promiseLibraryConnection(libraryId)
					.eventually { connectionProvider ->
						connectionProvider
							?.promiseFileUpdate(libraryId, serviceFile, property, value, isFormatted)
							.keepPromise(Unit)
					}
				else Unit.toPromise()
			}

	private fun ProvideConnections.promiseFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> {
		val promisedUpdate = promiseResponse("File/SetInfo", "File=${serviceFile.key}", "Field=$property", "Value=$value", "formatted=" + if (isFormatted) "1" else "0")
			.then { response ->
				response.use {
					logger.info("api/v1/File/SetInfo responded with a response code of {}.", it.code)
				}
			}

		checkRevisions.promiseRevision(libraryId)
			.then { revision ->
				val urlKeyHolder = UrlKeyHolder(urlProvider.baseUrl, serviceFile)
				filePropertiesContainerRepository.getFilePropertiesContainer(urlKeyHolder)
					?.takeIf { it.revision == revision }
					?.updateProperty(property, value)
				sendApplicationMessages.sendMessage(FilePropertiesUpdatedMessage(urlKeyHolder))
			}
			.excuse { e ->
				logger.warn(
					"${serviceFile.key}'s property cache item $property was not updated with the new value of $value",
					e
				)
			}

		return promisedUpdate
	}
}
