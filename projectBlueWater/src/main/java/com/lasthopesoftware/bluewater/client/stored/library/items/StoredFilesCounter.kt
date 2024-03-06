package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.CountStoredFiles
import com.namehillsoftware.handoff.promises.Promise

class StoredFilesCounter(private val storedFiles: AccessStoredFiles) : CountStoredFiles {
	override fun promiseStoredFilesCount(libraryId: LibraryId): Promise<Long> =
		storedFiles.promiseAllStoredFiles(libraryId).then { it -> it.size.toLong() }
}
