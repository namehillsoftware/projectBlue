package com.lasthopesoftware.bluewater.client.browsing.files.properties.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckScopedRevisions
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

private val logger by lazyLogger<ScopedFilePropertiesStorage>()

class ScopedFilePropertiesStorage(
	private val scopedConnectionProvider: IConnectionProvider,
	private val checkIfScopedConnectionIsReadOnly: CheckIfScopedConnectionIsReadOnly,
	private val checkScopedRevisions: CheckScopedRevisions,
	private val filePropertiesContainerRepository: IFilePropertiesContainerRepository,
	private val sendMessages: SendApplicationMessages
) : UpdateScopedFileProperties {

	override fun promiseFileUpdate(serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		checkIfScopedConnectionIsReadOnly.promiseIsReadOnly().eventually { isReadOnly ->

			if (isReadOnly) return@eventually Promise(SecurityException("Authentication is required to save properties"))

			val promisedUpdate = scopedConnectionProvider.promiseResponse(
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

			scopedConnectionProvider.urlProvider.baseUrl?.also { baseUrl ->
				val urlKeyHolder = UrlKeyHolder(baseUrl, serviceFile)
				checkScopedRevisions
					.promiseRevision()
					.then { revision ->
						filePropertiesContainerRepository.getFilePropertiesContainer(urlKeyHolder)
							?.takeIf { it.revision == revision }
							?.updateProperty(property, value)
						sendMessages.sendMessage(FilePropertiesUpdatedMessage(urlKeyHolder))
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
}
