package com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.supplier

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.access.ICachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.configuration.IDiskFileCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.disk.IDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.persistence.IDiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.CachedFileOutputStream
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class DiskFileCacheStreamSupplier(
    private val diskCacheDirectory: IDiskCacheDirectoryProvider,
    private val diskFileCacheConfiguration: IDiskFileCacheConfiguration,
    private val diskFileCachePersistence: IDiskFileCachePersistence,
    private val cachedFilesProvider: ICachedFilesProvider
) : SupplyCacheStreams {
    override fun promiseCachedFileOutputStream(uniqueKey: String): Promise<CacheOutputStream> {
        return cachedFilesProvider
            .promiseCachedFile(uniqueKey)
            .then { cachedFile: CachedFile? ->
                val file = if (cachedFile != null) File(cachedFile.fileName) else generateCacheFile(
                    uniqueKey
                )
                CachedFileOutputStream(uniqueKey, file, diskFileCachePersistence)
            }
    }

    private fun generateCacheFile(uniqueKey: String): File {
        val suffix = ".cache"
        val uniqueKeyHashCode = uniqueKey.hashCode().toString()
        val diskCacheDir = diskCacheDirectory.getDiskCacheDirectory(diskFileCacheConfiguration)
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
