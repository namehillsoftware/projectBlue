package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class StoredFilePathsLookup(
	private val libraryProvider: ILibraryProvider,
	private val storedFilePaths: ProduceStoredFilePaths,
) : GetStoredFilePaths {
	override fun promiseStoredFilePath(libraryId: LibraryId, serviceFile: ServiceFile): Promise<String?> =
		libraryProvider
			.getLibrary(libraryId)
			.eventually { library ->
				when (library?.syncedFileLocation) {
					null -> Promise.empty()
					else -> storedFilePaths.promiseStoredFilePath(libraryId, serviceFile)
				}
			}
}
