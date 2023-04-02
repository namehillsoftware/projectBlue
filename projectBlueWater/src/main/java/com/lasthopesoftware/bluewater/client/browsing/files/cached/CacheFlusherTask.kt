package com.lasthopesoftware.bluewater.client.browsing.files.cached

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.IDiskFileCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.IDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.CACHE_NAME
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.FILE_NAME
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.FILE_SIZE
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.LAST_ACCESSED_TIME
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.tableName
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
		RepositoryAccessHelper(context).use { repositoryAccess ->
			fun RepositoryAccessHelper.isCacheFull() = getCachedFileSizeFromDatabase() > targetSize

			if (!repositoryAccess.isCacheFull()) return
			while (repositoryAccess.isCacheFull()) {
				repositoryAccess.getOldestCachedFile()?.also { repositoryAccess.deleteCachedFile(it) }
			}

			// Remove any files in the cache dir but not in the database
			val filesInCacheDir = diskCacheDirectory
				.getDiskCacheDirectory(diskFileCacheConfiguration)
				?.takeIf { it.exists() }
				?.listFiles() ?: return

			// If the # of files in the cache dir is equal to the database size, then
			// hypothetically (and good enough for our purposes), they are in sync and we don't need
			// to do additional processing
			if (filesInCacheDir.size.toLong() == repositoryAccess.getCachedFileCount()) return

			// Remove all files that aren't tracked in the database
			for (fileInCacheDir in filesInCacheDir) {
				try {
					if (repositoryAccess.getCachedFileByFilename(fileInCacheDir.canonicalPath) != null) continue
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

	private fun RepositoryAccessHelper.getCachedFileSizeFromDatabase(): Long =
		mapSql("SELECT SUM($FILE_SIZE) FROM $tableName WHERE $CACHE_NAME = @$CACHE_NAME")
			.addParameter(CACHE_NAME, diskFileCacheConfiguration.cacheName)
			.execute()

	private fun RepositoryAccessHelper.getOldestCachedFile(): CachedFile? =
		mapSql("SELECT * FROM $tableName WHERE $CACHE_NAME = @$CACHE_NAME ORDER BY $LAST_ACCESSED_TIME ASC")
			.addParameter(CACHE_NAME, diskFileCacheConfiguration.cacheName)
			.fetchFirst(CachedFile::class.java)

	private fun RepositoryAccessHelper.getCachedFileCount(): Long =
		mapSql("SELECT COUNT(*) FROM $tableName WHERE $CACHE_NAME = @$CACHE_NAME")
			.addParameter(CACHE_NAME, diskFileCacheConfiguration.cacheName)
			.execute()

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(CacheFlusherTask::class.java) }

		fun promisedCacheFlushing(
			context: Context,
			diskCacheDirectory: IDiskCacheDirectoryProvider,
			diskFileCacheConfiguration: IDiskFileCacheConfiguration,
			targetSize: Long
		): Promise<*> =
			promiseTableMessage<Unit, CachedFile>(
				CacheFlusherTask(
					context,
					diskCacheDirectory,
					diskFileCacheConfiguration,
					targetSize
				)
			)

		private fun RepositoryAccessHelper.getCachedFileByFilename(fileName: String): CachedFile? =
			mapSql("SELECT * FROM $tableName WHERE $FILE_NAME = @$FILE_NAME")
				.addParameter(FILE_NAME, fileName)
				.fetchFirst(CachedFile::class.java)

		private fun RepositoryAccessHelper.deleteCachedFile(cachedFile: CachedFile): Boolean =
			((cachedFile.fileName?.let(::File)?.run { exists() && delete() } ?: false)
				and (mapSql("DELETE FROM $tableName WHERE id = @id")
				.addParameter("id", cachedFile.id)
				.execute() > 0))
	}
}
