package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface CountStoredFiles {
    fun promiseStoredFilesCount(libraryId: LibraryId): Promise<Long>
}
