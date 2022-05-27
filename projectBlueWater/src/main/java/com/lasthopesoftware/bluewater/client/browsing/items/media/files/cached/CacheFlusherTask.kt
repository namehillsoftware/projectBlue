package com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.configuration.IDiskFileCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.disk.IDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile.Companion.CACHE_NAME
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile.Companion.FILE_NAME
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile.Companion.FILE_SIZE
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile.Companion.LAST_ACCESSED_TIME
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile.Companion.tableName
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

/**
 * Flush a given cache until it reaches the given target size
 * @author david
 */
class CacheFlusherTask  /*
	 * Flush a given cache until it reaches the given target size
	 */ private constructor(
    private val context: Context,
    private val diskCacheDirectory: IDiskCacheDirectoryProvider,
    private val diskFileCacheConfiguration: IDiskFileCacheConfiguration,
    private val targetSize: Long
) : MessageWriter<Unit> {
    override fun prepareMessage() {
        flushCache()
    }

    private fun flushCache() {
        RepositoryAccessHelper(context).use { repositoryAccessHelper ->
            if (getCachedFileSizeFromDatabase(repositoryAccessHelper) <= targetSize) return
            do {
                val cachedFile = getOldestCachedFile(repositoryAccessHelper)
                if (cachedFile != null) deleteCachedFile(repositoryAccessHelper, cachedFile)
            } while (getCachedFileSizeFromDatabase(repositoryAccessHelper) > targetSize)

            // Remove any files in the cache dir but not in the database
            val cacheDir = diskCacheDirectory.getDiskCacheDirectory(diskFileCacheConfiguration)

            if (cacheDir == null || !cacheDir.exists()) return

			val filesInCacheDir = cacheDir.listFiles()

            // If the # of files in the cache dir is equal to the database size, then
            // hypothetically (and good enough for our purposes), they are in sync and we don't need
            // to do additional processing
            if (filesInCacheDir == null || filesInCacheDir.size.toLong() == getCachedFileCount(repositoryAccessHelper)) return

            // Remove all files that aren't tracked in the database
            for (fileInCacheDir in filesInCacheDir) {
                try {
                    if (getCachedFileByFilename(repositoryAccessHelper, fileInCacheDir.canonicalPath) != null) continue
                } catch (e: IOException) {
                    logger.warn("Issue getting canonical file path.", e)
                }

				if (fileInCacheDir.isDirectory) {
					try {
						FileUtils.deleteDirectory(fileInCacheDir)
					} catch (e: IOException) {
						logger.warn("The cache directory `${fileInCacheDir.path}` could not be deleted.", e)
					}
					continue
				}

                if (fileInCacheDir.delete()) continue

				logger.warn("The cached file `${fileInCacheDir.path}` could not be deleted.")
            }
        }
    }

    private fun getCachedFileSizeFromDatabase(repositoryAccessHelper: RepositoryAccessHelper): Long =
		repositoryAccessHelper
			.mapSql("SELECT SUM($FILE_SIZE) FROM $tableName WHERE $CACHE_NAME = @$CACHE_NAME")
			.addParameter(CACHE_NAME, diskFileCacheConfiguration.cacheName)
			.execute()

    //	private final long getCacheSizeBetweenTimes(final Dao<CachedFile, Integer> cachedFileAccess, final long startTime, final long endTime) {
    //		try {
    //
    //			final PreparedQuery<CachedFile> preparedQuery =
    //					cachedFileAccess.queryBuilder()
    //						.selectRaw("SUM(" + CachedFile.FILE_SIZE + ")")
    //						.where()
    //						.eq(CachedFile.CACHE_NAME, new SelectArg())
    //						.and()
    //						.between(CachedFile.CREATED_TIME, new SelectArg(), new SelectArg())
    //						.prepare();
    //
    //			return cachedFileAccess.queryRawValue(preparedQuery.getStatement(), cacheName, String.valueOf(startTime), String.valueOf(endTime));
    //		} catch (SQLException e) {
    //			logger.excuse("Error getting serviceFile size", e);
    //			return -1;
    //		}
    //	}
    private fun getOldestCachedFile(repositoryAccessHelper: RepositoryAccessHelper): CachedFile? =
		repositoryAccessHelper
			.mapSql("SELECT * FROM $tableName WHERE $CACHE_NAME = @$CACHE_NAME ORDER BY $LAST_ACCESSED_TIME ASC")
			.addParameter(CACHE_NAME, diskFileCacheConfiguration.cacheName)
			.fetchFirst(CachedFile::class.java)

    private fun getCachedFileCount(repositoryAccessHelper: RepositoryAccessHelper): Long = repositoryAccessHelper
		.mapSql("SELECT COUNT(*) FROM $tableName WHERE $CACHE_NAME = @$CACHE_NAME")
		.addParameter(CACHE_NAME, diskFileCacheConfiguration.cacheName)
		.execute()

    companion object {
        private val logger by lazy { LoggerFactory.getLogger(CacheFlusherTask::class.java) }

        fun promisedCacheFlushing(context: Context, diskCacheDirectory: IDiskCacheDirectoryProvider, diskFileCacheConfiguration: IDiskFileCacheConfiguration, targetSize: Long): Promise<*> =
			promiseTableMessage<Unit, CachedFile>(
				CacheFlusherTask(
					context,
					diskCacheDirectory,
					diskFileCacheConfiguration,
					targetSize
				)
			)

        private fun getCachedFileByFilename(repositoryAccessHelper: RepositoryAccessHelper, fileName: String): CachedFile? =
			repositoryAccessHelper
				.mapSql("SELECT * FROM $tableName WHERE $FILE_NAME = @$FILE_NAME")
				.addParameter(FILE_NAME, fileName)
				.fetchFirst(CachedFile::class.java)

        private fun deleteCachedFile(repositoryAccessHelper: RepositoryAccessHelper, cachedFile: CachedFile): Boolean =
			((cachedFile.fileName?.let { File(it) }?.let { it.exists() && it.delete() } ?: false)
					and (repositoryAccessHelper
				.mapSql("DELETE FROM $tableName WHERE id = @id")
				.addParameter("id", cachedFile.id)
				.execute() > 0))
    }
}
