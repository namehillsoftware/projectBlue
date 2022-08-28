package com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier

import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CacheOutputStream
import com.namehillsoftware.handoff.promises.Promise

interface SupplyCacheStreams {
    fun promiseCachedFileOutputStream(uniqueKey: String): Promise<CacheOutputStream>
}
