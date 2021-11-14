package com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise

interface GetAllStoredFilesInLibrary {
    fun promiseAllStoredFiles(libraryId: LibraryId): Promise<Collection<StoredFile>>
}
