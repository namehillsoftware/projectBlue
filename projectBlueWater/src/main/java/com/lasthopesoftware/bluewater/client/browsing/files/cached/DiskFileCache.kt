package com.lasthopesoftware.bluewater.client.browsing.files.cached

import android.content.Context
import android.database.SQLException
import com.lasthopesoftware.bluewater.client.browsing.files.cached.access.ProvideCachedFiles
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.DiskFileCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.ProvideDiskCacheDirectory
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.UpdateDiskFileAccessTime
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.SupplyCacheStreams
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.closables.eventuallyUse
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise
import java.io.File
import java.io.IOException

class DiskFileCache(
	private val context: Context,
	private val diskCacheDirectory: ProvideDiskCacheDirectory,
	private val diskFileCacheConfiguration: DiskFileCacheConfiguration,
	private val cacheStreamSupplier: SupplyCacheStreams,
	private val cachedFilesProvider: ProvideCachedFiles,
	private val diskFileAccessTimeUpdater: UpdateDiskFileAccessTime
) : CacheFiles {

	private val expirationTime = diskFileCacheConfiguration.cacheItemLifetime?.millis ?: -1

	override fun put(libraryId: LibraryId, uniqueKey: String, fileData: ByteArray): Promise<CachedFile?> {
		val putPromise = cacheStreamSupplier
			.promiseCachedFileOutputStream(libraryId, uniqueKey)
			.eventually { cachedFileOutputStream ->
				cachedFileOutputStream.eventuallyUse {
					writeCachedFileWithRetries(libraryId, uniqueKey, cachedFileOutputStream, fileData)
				}
			}

		putPromise.excuse { e ->
			logger.error("There was an error putting the cached file with the unique key $uniqueKey into the cache.", e)
		}
		return putPromise
	}

	private fun writeCachedFileWithRetries(libraryId: LibraryId, uniqueKey: String, cachedFileOutputStream: CacheOutputStream, fileData: ByteArray): Promise<CachedFile?> {
		return cachedFileOutputStream
			.promiseWrite(fileData, 0, fileData.size)
			.eventually { obj -> obj.flush() }
			.eventually(
				{ fos -> fos.commitToCache() },
				{ e ->
				logger.error("Unable to write to file!", e)

				when (e) {
					is IOException -> {
						// Check if free space is too low and then attempt to free up enough space
						// to store image
						freeDiskSpace
							.takeIf { it <= fileData.size }
							?.let { currentFreeDiskSpace ->
								val targetSize = diskFileCacheConfiguration.maxSize.coerceAtMost(currentFreeDiskSpace + fileData.size)
								CacheFlushing
									.promisedCacheFlushing(context, diskCacheDirectory, diskFileCacheConfiguration, targetSize)
									.eventually {
										if (freeDiskSpace > fileData.size) put(libraryId, uniqueKey, fileData)
										else Promise.empty()
									}
							}
							.keepPromise()
					}
					else -> Promise.empty()
				}
			})
	}

	override fun promiseCachedFile(libraryId: LibraryId, uniqueKey: String): Promise<File?> {
		return cachedFilesProvider
			.promiseCachedFile(libraryId, uniqueKey)
			.then { cachedFile ->
				cachedFile?.fileName?.let { fileName ->
					try {

						val returnFile = File(fileName)

						logger.info("Checking if " + cachedFile.fileName + " exists.")

						when {
							!returnFile.exists() -> {
								logger.warn("Cached file `" + cachedFile.fileName + "` doesn't exist! Removing from database.")

								deleteCachedFile(cachedFile.id)
								null
							}
							// Remove the cached file and return null if it's past its expired time
							expirationTime > -1 && cachedFile.createdTime < System.currentTimeMillis() - expirationTime -> {
								logger.info("Cached file $uniqueKey expired. Deleting.")

								promiseDeletedFile(cachedFile, returnFile)
								null
							}
							else -> {
								diskFileAccessTimeUpdater.promiseFileAccessedUpdate(cachedFile)
								logger.info("Returning cached file $uniqueKey")
								returnFile
							}
						}
					} catch (sqlException: SQLException) {
						logger.error("There was an error attempting to get the cached file $uniqueKey", sqlException)
						null
					}
				}
			}
	}

	private fun promiseDeletedFile(cachedFile: CachedFile, file: File): Promise<Long> {
		return ThreadPools.io.preparePromise { file.delete() || !file.exists() }
			.eventually { isDeleted ->
				if (isDeleted) deleteCachedFile(cachedFile.id)
				else {
					logger.warn("Unable to delete cached file " + file.absolutePath)
					Promise(-1L)
				}
			}
	}

	private fun deleteCachedFile(cachedFileId: Long): Promise<Long> =
		promiseTableMessage {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				try {
					repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
						logger.info("Deleting cached file with id $cachedFileId")
						if (logger.isDebugEnabled) logger.debug("Cached file count: " + getTotalCachedFileCount(repositoryAccessHelper))
						val executionResult = repositoryAccessHelper
							.mapSql("DELETE FROM " + CachedFile.tableName + " WHERE id = @id")
							.addParameter("id", cachedFileId)
							.execute()
						if (logger.isDebugEnabled) logger.debug("Cached file count: " + getTotalCachedFileCount(repositoryAccessHelper))
						closeableTransaction.setTransactionSuccessful()
						executionResult
					}
				} catch (sqlException: SQLException) {
					logger.warn(
						"There was an error trying to delete the cached file with id $cachedFileId",
						sqlException
					)
				}
			}
			-1L
		}

	private val freeDiskSpace: Long
		get() = diskCacheDirectory.getRootDiskCacheDirectory()?.usableSpace ?: 0L

	companion object {
		private val logger by lazyLogger<DiskFileCache>()

		private fun getTotalCachedFileCount(repositoryAccessHelper: RepositoryAccessHelper): Long {
			return repositoryAccessHelper.mapSql("SELECT COUNT(*) FROM " + CachedFile.tableName).execute()
		}
	}
}
