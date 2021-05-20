package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.RevisionChecker
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class FilePropertiesProvider(private val libraryConnections: ProvideLibraryConnections, private val filePropertiesContainerProvider: IFilePropertiesContainerRepository) : ProvideLibraryFileProperties {
	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>> =
		libraryConnections.promiseLibraryConnection(libraryId).eventually { connectionProvider ->
			connectionProvider?.let {
				RevisionChecker.promiseRevision(connectionProvider).eventually { revision ->
					val urlKeyHolder = UrlKeyHolder(connectionProvider.urlProvider.baseUrl, serviceFile)
					val filePropertiesContainer =
						filePropertiesContainerProvider.getFilePropertiesContainer(urlKeyHolder)

					if (filePropertiesContainer != null && filePropertiesContainer.properties.isNotEmpty() && revision == filePropertiesContainer.revision)
						filePropertiesContainer.properties.toPromise()
					else
						FilePropertiesPromise(connectionProvider, filePropertiesContainerProvider, serviceFile,	revision)
				}
			} ?: emptyMap<String, String>().toPromise()
		}
}
