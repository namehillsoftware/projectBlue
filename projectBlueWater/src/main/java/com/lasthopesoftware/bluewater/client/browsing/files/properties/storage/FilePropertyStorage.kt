package com.lasthopesoftware.bluewater.client.browsing.files.properties.storage

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.connection.live.eventuallyFromDataAccess
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

private val logger by lazyLogger<FilePropertyStorage>()

class FilePropertyStorage(
	private val libraryConnections: ProvideLibraryConnections,
	private val urlKeyProvider: ProvideUrlKey,
	private val checkIfConnectionIsReadOnly: CheckIfConnectionIsReadOnly,
	private val checkRevisions: CheckRevisions,
	private val filePropertiesContainerRepository: IFilePropertiesContainerRepository,
	private val sendApplicationMessages: SendApplicationMessages
) : UpdateFileProperties {
	override fun promiseFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		checkIfConnectionIsReadOnly
			.promiseIsReadOnly(libraryId)
			.cancelBackEventually { isReadOnly ->
				if (!isReadOnly) libraryConnections
					.promiseLibraryConnection(libraryId)
					.eventuallyFromDataAccess { access ->
						access
							?.promiseFileUpdate(libraryId, serviceFile, property, value, isFormatted)
							.keepPromise(Unit)
					}
				else Unit.toPromise()
			}

	private fun RemoteLibraryAccess.promiseFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		promiseFilePropertyUpdate(serviceFile, property, value, isFormatted)
			.cancelBackThen { it, _ ->
				urlKeyProvider
					.promiseUrlKey(libraryId, serviceFile)
					.eventually { maybeUrlKey ->
						maybeUrlKey?.let { urlKey ->
							checkRevisions
								.promiseRevision(libraryId)
								.then { revision ->
									filePropertiesContainerRepository.getFilePropertiesContainer(urlKey)
										?.takeIf { it.revision == revision }
										?.updateProperty(property, value)
									sendApplicationMessages.sendMessage(FilePropertiesUpdatedMessage(urlKey))
								}
						}.keepPromise()
					}
					.excuse { e ->
						logger.warn(
							"${serviceFile.key}'s property cache item $property was not updated with the new value of $value",
							e
						)
					}
				it
			}
}
