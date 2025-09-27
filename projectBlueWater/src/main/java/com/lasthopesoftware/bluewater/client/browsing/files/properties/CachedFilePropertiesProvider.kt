package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class CachedFilePropertiesProvider(
	private val urlKeys: ProvideUrlKey,
	private val filePropertiesProvider: ProvideLibraryFileProperties,
	private val filePropertiesContainerRepository: IFilePropertiesContainerRepository,
) : ProvideLibraryFileProperties {
	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>> {
		return urlKeys.promiseUrlKey(libraryId, serviceFile)
			.cancelBackEventually { urlKey ->
				urlKey
					?.let(filePropertiesContainerRepository::getFilePropertiesContainer)
					?.takeIf { it.properties.isNotEmpty() }?.properties?.toPromise()
					?: filePropertiesProvider.promiseFileProperties(libraryId, serviceFile)
			}
	}
}
