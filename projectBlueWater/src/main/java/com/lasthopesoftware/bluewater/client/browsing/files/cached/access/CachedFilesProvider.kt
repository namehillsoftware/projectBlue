package com.lasthopesoftware.bluewater.client.browsing.files.cached.access

import android.content.Context
import android.database.SQLException
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.DiskFileCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise

class CachedFilesProvider(
    private val context: Context,
    private val diskFileCacheConfiguration: DiskFileCacheConfiguration
) : ProvideCachedFiles {

	companion object {
		private val logger by lazyLogger<CachedFilesProvider>()
		private const val cachedFileFilter =
			" WHERE " + CachedFile.LIBRARY_ID + " = @" + CachedFile.LIBRARY_ID +
				" AND " + CachedFile.CACHE_NAME + " = @" + CachedFile.CACHE_NAME +
				" AND " + CachedFile.UNIQUE_KEY + " = @" + CachedFile.UNIQUE_KEY
	}

    override fun promiseCachedFile(libraryId: LibraryId, uniqueKey: String): Promise<CachedFile?> =
		promiseTableMessage<CachedFile?> { getCachedFile(libraryId, uniqueKey) }

    private fun getCachedFile(libraryId: LibraryId, uniqueKey: String): CachedFile? =
        RepositoryAccessHelper(context).use { repositoryAccessHelper ->
            try {
                repositoryAccessHelper.beginNonExclusiveTransaction()
                    .use { closeableNonExclusiveTransaction ->
                        val cachedFile = repositoryAccessHelper
                            .mapSql("SELECT * FROM " + CachedFile.tableName + cachedFileFilter)
                            .addParameter(CachedFile.LIBRARY_ID, libraryId.id)
                            .addParameter(CachedFile.CACHE_NAME, diskFileCacheConfiguration.cacheName)
                            .addParameter(CachedFile.UNIQUE_KEY, uniqueKey)
                            .fetchFirst(cls<CachedFile>())
                        closeableNonExclusiveTransaction.setTransactionSuccessful()
                        cachedFile
                    }
            } catch (sqlException: SQLException) {
                logger.error(
                    "There was an error getting the cached file with unique key $uniqueKey",
                    sqlException
                )
                null
            }
        }
}
