package com.lasthopesoftware.bluewater.client.browsing.files.cached.stream

import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.resources.io.PromisingWritableStream
import com.namehillsoftware.handoff.promises.Promise
import okio.BufferedSource

interface CacheWritableStream : PromisingWritableStream {
	fun promiseTransfer(bufferedSource: BufferedSource): Promise<CacheWritableStream>
    fun commitToCache(): Promise<CachedFile?>
}
