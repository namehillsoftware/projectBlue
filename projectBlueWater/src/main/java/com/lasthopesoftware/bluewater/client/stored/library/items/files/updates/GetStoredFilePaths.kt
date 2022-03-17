package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface GetStoredFilePaths {
	fun promiseStoredFilePath(libraryId: LibraryId, serviceFile: ServiceFile): Promise<String?>
}
