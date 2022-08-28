package com.lasthopesoftware.bluewater.client.browsing.files.cached

import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

interface CacheFiles {
    fun put(uniqueKey: String, fileData: ByteArray): Promise<CachedFile?>
    fun promiseCachedFile(uniqueKey: String): Promise<File?>
}
