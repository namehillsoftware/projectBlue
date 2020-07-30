package com.lasthopesoftware.bluewater.client.stored.library.items.files

import android.content.Context
import android.database.SQLException
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GetAllStoredFilesInLibrary
import com.lasthopesoftware.bluewater.repository.InsertBuilder.Companion.fromTable
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.UpdateBuilder
import com.lasthopesoftware.resources.executors.CachedSingleThreadExecutor
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import org.slf4j.LoggerFactory
import java.util.concurrent.Executor

class StoredFileAccess(
	private val context: Context,
	private val getAllStoredFilesInLibrary: GetAllStoredFilesInLibrary) : IStoredFileAccess {

	override fun getStoredFile(storedFileId: Int): Promise<StoredFile?> {
		return QueuedPromise(MessageWriter<StoredFile> { RepositoryAccessHelper(context).use { repositoryAccessHelper -> getStoredFile(repositoryAccessHelper, storedFileId) } }, storedFileAccessExecutor())
	}

	override fun getStoredFile(library: Library, serviceFile: ServiceFile): Promise<StoredFile?> {
		return getStoredFileTask(library, serviceFile)
	}

	private fun getStoredFileTask(library: Library, serviceFile: ServiceFile): Promise<StoredFile?> {
		return QueuedPromise(MessageWriter<StoredFile> { RepositoryAccessHelper(context).use { repositoryAccessHelper -> getStoredFile(library, repositoryAccessHelper, serviceFile) } }, storedFileAccessExecutor())
	}

	override val downloadingStoredFiles: Promise<List<StoredFile>>
		get() {
			return QueuedPromise(MessageWriter<List<StoredFile>> {
				RepositoryAccessHelper(context).use { repositoryAccessHelper ->
					repositoryAccessHelper
						.mapSql(
							selectFromStoredFiles + " WHERE " + StoredFileEntityInformation.isDownloadCompleteColumnName + " = @" + StoredFileEntityInformation.isDownloadCompleteColumnName)
						.addParameter(StoredFileEntityInformation.isDownloadCompleteColumnName, false)
						.fetch(StoredFile::class.java)
				}
			}, storedFileAccessExecutor())
		}

	override fun markStoredFileAsDownloaded(storedFile: StoredFile): Promise<StoredFile> {
		return QueuedPromise(MessageWriter {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					repositoryAccessHelper
						.mapSql(
							" UPDATE " + StoredFileEntityInformation.tableName +
								" SET " + StoredFileEntityInformation.isDownloadCompleteColumnName + " = 1" +
								" WHERE id = @id")
						.addParameter("id", storedFile.id)
						.execute()
					closeableTransaction.setTransactionSuccessful()
				}
			}
			storedFile.setIsDownloadComplete(true)
			storedFile
		}, storedFileAccessExecutor())
	}

	override fun addMediaFile(library: Library, serviceFile: ServiceFile, mediaFileId: Int, filePath: String): Promise<Unit> {
		return QueuedPromise(MessageWriter {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				var storedFile: StoredFile? = getStoredFile(library, repositoryAccessHelper, serviceFile)
				if (storedFile == null) {
					createStoredFile(library, repositoryAccessHelper, serviceFile)
					storedFile = getStoredFile(library, repositoryAccessHelper, serviceFile)
						?.setIsOwner(false)
						?.setIsDownloadComplete(true)
						?.setPath(filePath)
				}
				storedFile?.storedMediaId = mediaFileId
				updateStoredFile(repositoryAccessHelper, storedFile)
			}
		}, storedFileAccessExecutor())
	}

	override fun pruneStoredFiles(libraryId: LibraryId, serviceFilesToKeep: Set<ServiceFile>): Promise<Unit> {
		return getAllStoredFilesInLibrary.promiseAllStoredFiles(libraryId)
			.eventually(PruneFilesTask(this, serviceFilesToKeep))
	}

	private fun getStoredFile(library: Library, helper: RepositoryAccessHelper, serviceFile: ServiceFile): StoredFile? {
		return helper
			.mapSql(
				" SELECT * " +
					" FROM " + StoredFileEntityInformation.tableName + " " +
					" WHERE " + StoredFileEntityInformation.serviceIdColumnName + " = @" + StoredFileEntityInformation.serviceIdColumnName +
					" AND " + StoredFileEntityInformation.libraryIdColumnName + " = @" + StoredFileEntityInformation.libraryIdColumnName)
			.addParameter(StoredFileEntityInformation.serviceIdColumnName, serviceFile.key)
			.addParameter(StoredFileEntityInformation.libraryIdColumnName, library.id)
			.fetchFirst(StoredFile::class.java)
	}

	private fun getStoredFile(helper: RepositoryAccessHelper, storedFileId: Int): StoredFile {
		return helper
			.mapSql("SELECT * FROM " + StoredFileEntityInformation.tableName + " WHERE id = @id")
			.addParameter("id", storedFileId)
			.fetchFirst(StoredFile::class.java)
	}

	private fun createStoredFile(library: Library, repositoryAccessHelper: RepositoryAccessHelper, serviceFile: ServiceFile) {
		repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
			repositoryAccessHelper
				.mapSql(insertSql.value)
				.addParameter(StoredFileEntityInformation.serviceIdColumnName, serviceFile.key)
				.addParameter(StoredFileEntityInformation.libraryIdColumnName, library.id)
				.addParameter(StoredFileEntityInformation.isOwnerColumnName, true)
				.execute()
			closeableTransaction.setTransactionSuccessful()
		}
	}

	fun deleteStoredFile(storedFile: StoredFile) {
		storedFileAccessExecutor().execute {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				try {
					repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
						repositoryAccessHelper
							.mapSql("DELETE FROM " + StoredFileEntityInformation.tableName + " WHERE id = @id")
							.addParameter("id", storedFile.id)
							.execute()
						closeableTransaction.setTransactionSuccessful()
					}
				} catch (e: SQLException) {
					logger.error("There was an error deleting serviceFile " + storedFile.id, e)
				}
			}
		}
	}

	companion object {
		@JvmStatic
		fun storedFileAccessExecutor(): Executor {
			return storedFileAccessExecutor.value
		}

		private val storedFileAccessExecutor = lazy { CachedSingleThreadExecutor() }
		private val logger = LoggerFactory.getLogger(StoredFileAccess::class.java)
		private const val selectFromStoredFiles = "SELECT * FROM " + StoredFileEntityInformation.tableName

		private val insertSql = lazy {
			fromTable(StoredFileEntityInformation.tableName)
				.addColumn(StoredFileEntityInformation.serviceIdColumnName)
				.addColumn(StoredFileEntityInformation.libraryIdColumnName)
				.addColumn(StoredFileEntityInformation.isOwnerColumnName)
				.build()
		}

		private val updateSql = lazy {
			UpdateBuilder
				.fromTable(StoredFileEntityInformation.tableName)
				.addSetter(StoredFileEntityInformation.serviceIdColumnName)
				.addSetter(StoredFileEntityInformation.storedMediaIdColumnName)
				.addSetter(StoredFileEntityInformation.pathColumnName)
				.addSetter(StoredFileEntityInformation.isOwnerColumnName)
				.addSetter(StoredFileEntityInformation.isDownloadCompleteColumnName)
				.setFilter("WHERE id = @id")
				.buildQuery()
		}

		private fun updateStoredFile(repositoryAccessHelper: RepositoryAccessHelper, storedFile: StoredFile?) {
			repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
				repositoryAccessHelper
					.mapSql(updateSql.value)
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, storedFile!!.serviceId)
					.addParameter(StoredFileEntityInformation.storedMediaIdColumnName, storedFile.storedMediaId)
					.addParameter(StoredFileEntityInformation.pathColumnName, storedFile.path)
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, storedFile.isOwner)
					.addParameter(StoredFileEntityInformation.isDownloadCompleteColumnName, storedFile.isDownloadComplete)
					.addParameter("id", storedFile.id)
					.execute()
				closeableTransaction.setTransactionSuccessful()
			}
		}
	}

}
