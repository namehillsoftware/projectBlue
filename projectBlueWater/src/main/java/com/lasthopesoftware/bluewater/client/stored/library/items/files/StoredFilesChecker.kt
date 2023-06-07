package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class StoredFilesChecker(private val countStoredFiles: CountStoredFiles) : CheckForAnyStoredFiles {
    override fun promiseIsAnyStoredFiles(libraryId: LibraryId): Promise<Boolean> =
		countStoredFiles.promiseStoredFilesCount(libraryId).then { count -> count > 0 }
}
