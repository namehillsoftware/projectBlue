package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class FilePropertiesProvider(private val libraryConnections: ProvideLibraryConnections, private val checkRevisions: CheckRevisions, private val filePropertiesContainerProvider: IFilePropertiesContainerRepository) : ProvideLibraryFileProperties {

	companion object {
		private val emptyProperties = lazy { Promise(emptyMap<String, String>()) }
	}

	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>> {
		val promisedRevision = checkRevisions.promiseRevision(libraryId)

		return libraryConnections.promiseLibraryConnection(libraryId).eventually { connectionProvider ->
			connectionProvider?.urlProvider?.baseUrl?.let { baseUrl ->
				val urlKeyHolder = UrlKeyHolder(baseUrl, serviceFile)
				val filePropertiesContainer =
					filePropertiesContainerProvider.getFilePropertiesContainer(urlKeyHolder)

				promisedRevision.eventually { revision ->
					if (filePropertiesContainer != null && filePropertiesContainer.properties.isNotEmpty() && revision == filePropertiesContainer.revision)
						filePropertiesContainer.properties.toPromise()
					else
						FilePropertiesPromise(connectionProvider, filePropertiesContainerProvider, serviceFile,	revision)
				}
			} ?: emptyProperties.value
		}
	}
}
