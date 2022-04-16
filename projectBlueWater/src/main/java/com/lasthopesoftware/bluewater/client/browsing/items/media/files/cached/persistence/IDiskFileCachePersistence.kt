package com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.persistence

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

interface IDiskFileCachePersistence {
    fun putIntoDatabase(uniqueKey: String, file: File): Promise<CachedFile>
}
