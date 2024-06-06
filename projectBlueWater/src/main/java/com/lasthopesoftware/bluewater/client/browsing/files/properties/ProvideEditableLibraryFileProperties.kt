package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideEditableLibraryFileProperties {
	fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Sequence<FileProperty>>
}
