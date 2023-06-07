package com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence

import android.content.Context
import android.database.SQLException
import com.lasthopesoftware.bluewater.client.browsing.files.cached.CacheFlusherTask
import com.lasthopesoftware.bluewater.client.browsing.files.cached.access.ICachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.IDiskFileCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.IDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.CACHE_NAME
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.CREATED_TIME
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.FILE_NAME
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.FILE_SIZE
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.LAST_ACCESSED_TIME
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.LIBRARY_ID
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.UNIQUE_KEY
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile.Companion.tableName
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.repository.DatabasePromise
import com.lasthopesoftware.bluewater.repository.InsertBuilder.Companion.fromTable
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

class DiskFileCachePersistence(
	private val context: Context,
	private val diskCacheDirectoryProvider: IDiskCacheDirectoryProvider,
	private val diskFileCacheConfiguration: IDiskFileCacheConfiguration,
	private val cachedFilesProvider: ICachedFilesProvider,
	private val diskFileAccessTimeUpdater: IDiskFileAccessTimeUpdater
) : IDiskFileCachePersistence {
	override fun putIntoDatabase(libraryId: LibraryId, uniqueKey: String, file: File): Promise<CachedFile> {
		val canonicalFilePath = try {
			file.canonicalPath
		} catch (e: IOException) {
			logger.error("There was an error getting the canonical path for $file", e)
			return Promise.empty()
		}

		return cachedFilesProvider
			.promiseCachedFile(libraryId, uniqueKey)
			.eventually { cachedFile ->
				cachedFile
					?.let {
						if (it.fileName == canonicalFilePath) diskFileAccessTimeUpdater.promiseFileAccessedUpdate(it)
						else promiseFilePathUpdate(it).eventually(diskFileAccessTimeUpdater::promiseFileAccessedUpdate)
					}
					?: promiseTableMessage<Unit, CachedFile> {
						logger.info("File with unique key $uniqueKey doesn't exist. Creating...")
						try {
							RepositoryAccessHelper(context).use { repositoryAccessHelper ->
								try {
									repositoryAccessHelper.beginTransaction()
										.use { closeableTransaction ->
											val currentTimeMillis = System.currentTimeMillis()
											repositoryAccessHelper.mapSql(cachedFileSqlInsert)
												.addParameter(FILE_NAME, canonicalFilePath)
												.addParameter(CACHE_NAME, diskFileCacheConfiguration.cacheName)
												.addParameter(FILE_SIZE, file.length())
												.addParameter(LIBRARY_ID, libraryId.id)
												.addParameter(UNIQUE_KEY, uniqueKey)
												.addParameter(CREATED_TIME, currentTimeMillis)
												.addParameter(LAST_ACCESSED_TIME, currentTimeMillis)
												.execute()
											closeableTransaction.setTransactionSuccessful()
										}
								} catch (sqlException: SQLException) {
									logger.warn("There was an error inserting the cached file with the unique key $uniqueKey", sqlException)
									throw sqlException
								}
							}
						} finally {
							CacheFlusherTask.promisedCacheFlushing(context, diskCacheDirectoryProvider, diskFileCacheConfiguration, diskFileCacheConfiguration.maxSize)
						}
					}.eventually { cachedFilesProvider.promiseCachedFile(libraryId, uniqueKey) }
			}
	}

	private fun promiseFilePathUpdate(cachedFile: CachedFile): Promise<CachedFile> = DatabasePromise {
		val cachedFileId = cachedFile.id
		val cachedFilePath = cachedFile.fileName
		RepositoryAccessHelper(context).use { repositoryAccessHelper ->
			try {
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					logger.info("Updating file name of cached file with ID $cachedFileId to $cachedFilePath")
					repositoryAccessHelper
						.mapSql("UPDATE $tableName SET $FILE_NAME = @$FILE_NAME WHERE id = @id")
						.addParameter(FILE_NAME, cachedFilePath)
						.addParameter("id", cachedFileId)
						.execute()
					closeableTransaction.setTransactionSuccessful()
				}
			} catch (sqlException: SQLException) {
				logger.error(
					"There was an error trying to update the cached file with ID $cachedFileId",
					sqlException
				)
				throw sqlException
			}
		}
		cachedFile
	}

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(DiskFileCachePersistence::class.java) }
		private val cachedFileSqlInsert by lazy {
			fromTable(tableName)
				.addColumn(CACHE_NAME)
				.addColumn(FILE_NAME)
				.addColumn(FILE_SIZE)
				.addColumn(LIBRARY_ID)
				.addColumn(UNIQUE_KEY)
				.addColumn(CREATED_TIME)
				.addColumn(LAST_ACCESSED_TIME)
				.withReplacement()
				.build()
		}
	}
}
