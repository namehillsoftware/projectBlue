package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class SelectedLibraryFilePropertiesProvider(
	private val selectedLibraryId: ProvideSelectedLibraryId,
	private val provideLibraryFileProperties: ProvideLibraryFileProperties
) : ProvideScopedFileProperties {
	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> =
		selectedLibraryId
			.promiseSelectedLibraryId()
			.eventually { response ->
				response
					?.let { libraryId -> provideLibraryFileProperties.promiseFileProperties(libraryId, serviceFile) }
					.keepPromise(emptyMap())
			}
}
