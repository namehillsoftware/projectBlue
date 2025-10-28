package com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier

import com.lasthopesoftware.bluewater.client.browsing.files.cached.access.ProvideCachedFiles
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.ProvideDiskCacheDirectory
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.IDiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CacheWritableStream
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CachedFileWritableStream
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class DiskFileCacheStreamSupplier(
    private val diskCacheDirectory: ProvideDiskCacheDirectory,
    private val diskFileCachePersistence: IDiskFileCachePersistence,
    private val cachedFilesProvider: ProvideCachedFiles
) : SupplyCacheStreams {
    override fun promiseCachedFileOutputStream(libraryId: LibraryId, uniqueKey: String): Promise<CacheWritableStream> {
        return cachedFilesProvider
            .promiseCachedFile(libraryId, uniqueKey)
            .then { cachedFile ->
                val file = cachedFile?.fileName?.let(::File) ?: generateCacheFile(libraryId, uniqueKey)
                CachedFileWritableStream(libraryId, uniqueKey, file, diskFileCachePersistence)
            }
    }

    private fun generateCacheFile(libraryId: LibraryId, uniqueKey: String): File {
        val suffix = ".cache"
        val uniqueKeyHashCode = uniqueKey.hashCode().toString()
        val diskCacheDir = diskCacheDirectory.getLibraryDiskCacheDirectory(libraryId)
        var file = File(diskCacheDir, uniqueKeyHashCode + suffix)
        if (file.exists()) {
            var collisionNumber = 0
            do {
                file = File(diskCacheDir, uniqueKeyHashCode + "-" + collisionNumber++ + suffix)
            } while (file.exists())
        }
        return file
    }
}
