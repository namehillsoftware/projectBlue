package com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached

import android.content.Context
import android.database.SQLException
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.access.ICachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.configuration.IDiskFileCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.disk.IDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.persistence.IDiskFileAccessTimeUpdater
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.supplier.ICacheStreamSupplier
import com.lasthopesoftware.bluewater.repository.DatabasePromise
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

class DiskFileCache(private val context: Context, private val diskCacheDirectory: IDiskCacheDirectoryProvider, private val diskFileCacheConfiguration: IDiskFileCacheConfiguration, private val cacheStreamSupplier: ICacheStreamSupplier, private val cachedFilesProvider: ICachedFilesProvider, private val diskFileAccessTimeUpdater: IDiskFileAccessTimeUpdater) : ICache {

	private val expirationTime = diskFileCacheConfiguration.cacheItemLifetime?.millis ?: -1

	override fun put(uniqueKey: String, fileData: ByteArray): Promise<CachedFile> {
		val putPromise = cacheStreamSupplier
			.promiseCachedFileOutputStream(uniqueKey)
			.eventually { cachedFileOutputStream -> writeCachedFileWithRetries(uniqueKey, cachedFileOutputStream, fileData) }

		putPromise.excuse { e ->
			logger.error("There was an error putting the cached file with the unique key $uniqueKey into the cache.", e)
		}
		return putPromise
	}

	private fun writeCachedFileWithRetries(uniqueKey: String, cachedFileOutputStream: CacheOutputStream, fileData: ByteArray): Promise<CachedFile> {
		return cachedFileOutputStream
			.promiseWrite(fileData, 0, fileData.size)
			.eventually { obj -> obj.flush() }
			.eventually({ fos ->
				fos.commitToCache()
			}) { e ->
				logger.error("Unable to write to file!", e)

				when (e) {
					is IOException -> {
						// Check if free space is too low and then attempt to free up enough space
						// to store image
						freeDiskSpace
							.takeIf { it <= fileData.size }
							?.let { currentFreeDiskSpace ->
								val targetSize = diskFileCacheConfiguration.maxSize.coerceAtMost(currentFreeDiskSpace + fileData.size)
								CacheFlusherTask
									.promisedCacheFlushing(context, diskCacheDirectory, diskFileCacheConfiguration, targetSize)
									.eventually {
										if (freeDiskSpace > fileData.size) put(uniqueKey, fileData)
										else Promise.empty()
									}
							}
							.keepPromise()
					}
					else -> Promise.empty()
				}
			}
			.must { cachedFileOutputStream.close() }
	}

	override fun promiseCachedFile(uniqueKey: String): Promise<File?> {
		return cachedFilesProvider
			.promiseCachedFile(uniqueKey)
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
		return QueuedPromise(MessageWriter { file.delete() || !file.exists() }, ThreadPools.io)
			.eventually { isDeleted ->
				if (isDeleted) deleteCachedFile(cachedFile.id)
				else {
					logger.warn("Unable to delete cached file " + file.absolutePath)
					Promise(-1L)
				}
			}
	}

	private fun deleteCachedFile(cachedFileId: Long): Promise<Long> =
		DatabasePromise {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				try {
					repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
						logger.info("Deleting cached file with id $cachedFileId")
						if (logger.isDebugEnabled) logger.debug(
							"Cached file count: " + getTotalCachedFileCount(
								repositoryAccessHelper
							)
						)
						val executionResult = repositoryAccessHelper
							.mapSql("DELETE FROM " + CachedFile.tableName + " WHERE id = @id")
							.addParameter("id", cachedFileId)
							.execute()
						if (logger.isDebugEnabled) logger.debug(
							"Cached file count: " + getTotalCachedFileCount(
								repositoryAccessHelper
							)
						)
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
		get() = diskCacheDirectory.getDiskCacheDirectory(diskFileCacheConfiguration).usableSpace

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(DiskFileCache::class.java) }

		private fun getTotalCachedFileCount(repositoryAccessHelper: RepositoryAccessHelper): Long {
			return repositoryAccessHelper.mapSql("SELECT COUNT(*) FROM " + CachedFile.tableName).execute()
		}
	}
}
