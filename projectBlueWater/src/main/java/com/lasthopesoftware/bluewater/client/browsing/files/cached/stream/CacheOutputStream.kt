package com.lasthopesoftware.bluewater.client.browsing.files.cached.stream

import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.resources.io.PromisingOutputStream
import com.namehillsoftware.handoff.promises.Promise
import okio.BufferedSource

interface CacheOutputStream : PromisingOutputStream<CacheOutputStream> {
	fun promiseTransfer(bufferedSource: BufferedSource): Promise<CacheOutputStream>
    fun commitToCache(): Promise<CachedFile?>
}
