package com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence

import android.content.Context
import android.database.SQLException
import com.lasthopesoftware.bluewater.client.browsing.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise
import java.util.Date

class DiskFileAccessTimeUpdater(private val context: Context) : UpdateDiskFileAccessTime {

	companion object {
		private val logger by lazyLogger<DiskFileAccessTimeUpdater>()
	}

	override fun promiseFileAccessedUpdate(cachedFile: CachedFile): Promise<CachedFile> = promiseTableMessage<CachedFile, CachedFile> {
		doFileAccessedUpdate(cachedFile.id)
		cachedFile
	}

	private fun doFileAccessedUpdate(cachedFileId: Long) {
		val updateTime = System.currentTimeMillis()
		logger.info("Updating accessed time on cached file with ID $cachedFileId to " + Date(updateTime))
		RepositoryAccessHelper(context).use { repositoryAccessHelper ->
			try {
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					repositoryAccessHelper
						.mapSql("UPDATE " + CachedFile.tableName + " SET " + CachedFile.LAST_ACCESSED_TIME + " = @" + CachedFile.LAST_ACCESSED_TIME + " WHERE id = @id")
						.addParameter(CachedFile.LAST_ACCESSED_TIME, updateTime)
						.addParameter("id", cachedFileId)
						.execute()
					closeableTransaction.setTransactionSuccessful()
				}
			} catch (sqlException: SQLException) {
				logger.error("There was an error trying to update the cached file with ID $cachedFileId", sqlException)
				throw sqlException
			}
		}
	}
}
