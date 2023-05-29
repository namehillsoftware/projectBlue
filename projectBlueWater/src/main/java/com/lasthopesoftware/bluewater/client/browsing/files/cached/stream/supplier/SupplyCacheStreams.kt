package com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier

import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface SupplyCacheStreams {
    fun promiseCachedFileOutputStream(libraryId: LibraryId, uniqueKey: String): Promise<CacheOutputStream>
}
