package com.lasthopesoftware.bluewater.client.browsing.files.cached

import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

interface CacheFiles {
    fun put(libraryId: LibraryId, uniqueKey: String, fileData: ByteArray): Promise<CachedFile?>
    fun promiseCachedFile(libraryId: LibraryId, uniqueKey: String): Promise<File?>
}
