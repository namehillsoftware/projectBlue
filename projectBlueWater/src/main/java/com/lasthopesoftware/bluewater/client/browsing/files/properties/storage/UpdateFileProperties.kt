package com.lasthopesoftware.bluewater.client.browsing.files.properties.storage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface UpdateFileProperties {
	fun promiseFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit>
}
