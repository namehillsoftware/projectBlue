package com.lasthopesoftware.bluewater.client.browsing.files.cached.stream

import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.namehillsoftware.handoff.promises.Promise
import okio.BufferedSource
import java.io.Closeable

interface CacheOutputStream : Closeable {
    fun promiseWrite(buffer: ByteArray, offset: Int, length: Int): Promise<CacheOutputStream>
    fun promiseTransfer(bufferedSource: BufferedSource): Promise<CacheOutputStream>
    fun commitToCache(): Promise<CachedFile>
    fun flush(): Promise<CacheOutputStream>
}
