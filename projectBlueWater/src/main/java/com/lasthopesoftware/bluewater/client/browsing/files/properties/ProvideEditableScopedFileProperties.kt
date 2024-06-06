package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideEditableScopedFileProperties {
	fun promiseFileProperties(serviceFile: ServiceFile): Promise<Sequence<FileProperty>>
}

interface ProvideEditableLibraryFileProperties {
	fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Sequence<FileProperty>>
}
