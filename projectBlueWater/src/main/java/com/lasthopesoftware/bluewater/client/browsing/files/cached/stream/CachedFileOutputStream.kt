package com.lasthopesoftware.bluewater.client.browsing.files.cached.stream

import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.IDiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import okio.BufferedSource
import okio.sink
import java.io.File
import java.io.FileOutputStream

class CachedFileOutputStream(
    private val uniqueKey: String,
    private val file: File,
    private val diskFileCachePersistence: IDiskFileCachePersistence
) : CacheOutputStream {

	@Volatile
	private var isClosed = false

    private val lazyFileOutputStream = lazy { FileOutputStream(file) }

    override fun promiseWrite(
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): Promise<CacheOutputStream> {
        return QueuedPromise(MessageWriter {
            lazyFileOutputStream.value.write(buffer, offset, length)
            this
        }, ThreadPools.io)
    }

    override fun promiseTransfer(bufferedSource: BufferedSource): Promise<CacheOutputStream> {
        return QueuedPromise(MessageWriter {
            bufferedSource
                .readAll(lazyFileOutputStream.value.sink())
            this
        }, ThreadPools.io)
    }

    override fun flush(): Promise<CacheOutputStream> {
        return QueuedPromise(MessageWriter {
            if (lazyFileOutputStream.isInitialized()) lazyFileOutputStream.value.flush()
            this
        }, ThreadPools.io)
    }

    override fun commitToCache(libraryId: LibraryId): Promise<CachedFile> =
		if (!isClosed) diskFileCachePersistence.putIntoDatabase(libraryId, uniqueKey, file)
		else Promise.empty()

	override fun close() {
		isClosed = true
		if (lazyFileOutputStream.isInitialized()) lazyFileOutputStream.value.close()
	}
}
