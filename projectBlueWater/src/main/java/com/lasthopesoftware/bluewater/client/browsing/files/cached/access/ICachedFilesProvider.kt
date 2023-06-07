package com.lasthopesoftware.bluewater.client.browsing.files.cached.access

import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ICachedFilesProvider {
    fun promiseCachedFile(libraryId: LibraryId, uniqueKey: String): Promise<CachedFile?>
}
