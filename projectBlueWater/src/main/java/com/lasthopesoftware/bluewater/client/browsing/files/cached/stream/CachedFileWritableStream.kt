package com.lasthopesoftware.bluewater.client.browsing.files.cached.stream

import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.IDiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.guaranteedUnitResponse
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import java.io.File
import java.io.FileOutputStream

class CachedFileWritableStream(
	private val libraryId: LibraryId,
    private val uniqueKey: String,
    private val file: File,
    private val diskFileCachePersistence: IDiskFileCachePersistence
) : CacheWritableStream {

	@Volatile
	private var isClosed = false

    private val lazyFileOutputStream = lazy { FileOutputStream(file) }

    override fun promiseWrite(
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): Promise<Int> {
        return ThreadPools.io.preparePromise {
			if (!isClosed) {
				lazyFileOutputStream.value.write(buffer, offset, length)
				length
			} else 0
        }
    }

	override fun promiseFlush(): Promise<Unit> {
        return ThreadPools.io.preparePromise {
            if (!isClosed && lazyFileOutputStream.isInitialized()) lazyFileOutputStream.value.flush()
        }
    }

    override fun commitToCache(): Promise<CachedFile?> =
		if (!isClosed) diskFileCachePersistence.putIntoDatabase(libraryId, uniqueKey, file)
		else Promise.empty()

	override fun promiseClose(): Promise<Unit> {
		return promiseFlush().must { _ ->
			isClosed = true
			if (lazyFileOutputStream.isInitialized()) lazyFileOutputStream.value.close()
		}.guaranteedUnitResponse()
	}
}
