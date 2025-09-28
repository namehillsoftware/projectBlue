package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.namehillsoftware.handoff.promises.Promise

class EditableLibraryFilePropertiesProvider(
	private val inner: ProvideFreshLibraryFileProperties,
): ProvideEditableLibraryFileProperties {
	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Sequence<FileProperty>> =
		inner
			.promiseFileProperties(libraryId, serviceFile)
			.cancelBackThen { properties, signal -> properties.allProperties }
}
