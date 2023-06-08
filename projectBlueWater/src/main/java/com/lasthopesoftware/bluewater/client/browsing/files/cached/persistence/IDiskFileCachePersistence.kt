package com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence

import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

interface IDiskFileCachePersistence {
    fun putIntoDatabase(libraryId: LibraryId, uniqueKey: String, file: File): Promise<CachedFile?>
}
