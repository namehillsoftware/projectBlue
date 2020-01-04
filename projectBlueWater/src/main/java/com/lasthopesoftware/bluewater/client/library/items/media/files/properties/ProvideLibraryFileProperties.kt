package com.lasthopesoftware.bluewater.client.library.items.media.files.properties

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLibraryFileProperties {
	fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>>
}
