package com.lasthopesoftware.bluewater.client.stored.library.items.files

import android.content.Context
import android.database.SQLException
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation.isDownloadCompleteColumnName
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation.libraryIdColumnName
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation.tableName
import com.lasthopesoftware.bluewater.repository.InsertBuilder.Companion.fromTable
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.UpdateBuilder
import com.lasthopesoftware.bluewater.repository.fetch
import com.lasthopesoftware.bluewater.repository.fetchFirst
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory

class StoredFileAccess(private val context: Context) : AccessStoredFiles {

	override fun getStoredFile(storedFileId: Int): Promise<StoredFile?> =
		promiseTableMessage<StoredFile?, StoredFile> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				getStoredFile(repositoryAccessHelper, storedFileId)
			}
		}

	override fun getStoredFile(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile?> =
		getStoredFileTask(libraryId, serviceFile)

	override fun promiseAllStoredFiles(libraryId: LibraryId): Promise<Collection<StoredFile>> =
		promiseTableMessage<Collection<StoredFile>, StoredFile> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginNonExclusiveTransaction().use {
					repositoryAccessHelper
						.mapSql("SELECT * FROM $tableName WHERE $libraryIdColumnName = @$libraryIdColumnName")
						.addParameter(libraryIdColumnName, libraryId.id)
						.fetch()
				}
			}
		}

	override fun promiseDanglingFiles(): Promise<Collection<StoredFile>> =
		promiseTableMessage<Collection<StoredFile>, StoredFile> {
			RepositoryAccessHelper(context).use { helper ->
				helper
					.mapSql(
						"""SELECT DISTINCT *
							FROM $tableName
							WHERE $libraryIdColumnName NOT IN (
								SELECT id FROM ${LibraryEntityInformation.tableName})"""
					)
					.fetch()
			}
		}

	private fun getStoredFileTask(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile?> =
		promiseTableMessage<StoredFile?, StoredFile> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				getStoredFile(
					libraryId,
					repositoryAccessHelper,
					serviceFile
				)
			}
		}

	override fun promiseDownloadingFiles(): Promise<List<StoredFile>> =
		promiseTableMessage<List<StoredFile>, StoredFile> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper
					.mapSql(
						"$selectFromStoredFiles WHERE $isDownloadCompleteColumnName = @$isDownloadCompleteColumnName"
					)
					.addParameter(isDownloadCompleteColumnName, false)
					.fetch()
			}
		}

	override fun markStoredFileAsDownloaded(storedFile: StoredFile): Promise<StoredFile> =
		promiseTableMessage<StoredFile, StoredFile> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					repositoryAccessHelper
						.mapSql(
							" UPDATE " + tableName +
								" SET " + isDownloadCompleteColumnName + " = 1" +
								" WHERE id = @id"
						)
						.addParameter("id", storedFile.id)
						.execute()
					closeableTransaction.setTransactionSuccessful()
				}
			}
			storedFile.setIsDownloadComplete(true)
		}

	private fun getStoredFile(library: LibraryId, helper: RepositoryAccessHelper, serviceFile: ServiceFile): StoredFile? =
		helper.beginNonExclusiveTransaction().use {
			helper
				.mapSql(
					" SELECT * " +
						" FROM " + tableName + " " +
						" WHERE " + StoredFileEntityInformation.serviceIdColumnName + " = @" + StoredFileEntityInformation.serviceIdColumnName +
						" AND " + libraryIdColumnName + " = @" + libraryIdColumnName
				)
				.addParameter(StoredFileEntityInformation.serviceIdColumnName, serviceFile.key)
				.addParameter(libraryIdColumnName, library.id)
				.fetchFirst()
		}

	private fun getStoredFile(helper: RepositoryAccessHelper, storedFileId: Int): StoredFile? =
		helper.beginNonExclusiveTransaction().use {
			helper
				.mapSql("SELECT * FROM " + tableName + " WHERE id = @id")
				.addParameter("id", storedFileId)
				.fetchFirst()
		}

	private fun createStoredFile(libraryId: LibraryId, repositoryAccessHelper: RepositoryAccessHelper, serviceFile: ServiceFile) =
		repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
			repositoryAccessHelper
				.mapSql(insertSql)
				.addParameter(StoredFileEntityInformation.serviceIdColumnName, serviceFile.key)
				.addParameter(libraryIdColumnName, libraryId.id)
				.addParameter(StoredFileEntityInformation.isOwnerColumnName, true)
				.execute()
			closeableTransaction.setTransactionSuccessful()
		}

	override fun deleteStoredFile(storedFile: StoredFile): Promise<Unit> =
		promiseTableMessage<Unit, StoredFile> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				try {
					repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
						repositoryAccessHelper
							.mapSql("DELETE FROM " + tableName + " WHERE id = @id")
							.addParameter("id", storedFile.id)
							.execute()
						closeableTransaction.setTransactionSuccessful()
					}
				} catch (e: SQLException) {
					logger.error("There was an error deleting serviceFile " + storedFile.id, e)
				}
			}
		}

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(StoredFileAccess::class.java) }
		private const val selectFromStoredFiles = "SELECT * FROM " + tableName

		private val insertSql by lazy {
			fromTable(tableName)
				.addColumn(StoredFileEntityInformation.serviceIdColumnName)
				.addColumn(libraryIdColumnName)
				.addColumn(StoredFileEntityInformation.isOwnerColumnName)
				.build()
		}

		private val updateSql by lazy {
			UpdateBuilder
				.fromTable(tableName)
				.addSetter(StoredFileEntityInformation.serviceIdColumnName)
				.addSetter(StoredFileEntityInformation.uriColumnName)
				.addSetter(StoredFileEntityInformation.isOwnerColumnName)
				.addSetter(isDownloadCompleteColumnName)
				.setFilter("WHERE id = @id")
				.buildQuery()
		}

		private fun updateStoredFile(repositoryAccessHelper: RepositoryAccessHelper, storedFile: StoredFile?) {
			if (storedFile == null) return

			repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
				repositoryAccessHelper
					.mapSql(updateSql)
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, storedFile.serviceId)
					.addParameter(StoredFileEntityInformation.uriColumnName, storedFile.uri)
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, storedFile.isOwner)
					.addParameter(
						isDownloadCompleteColumnName,
						storedFile.isDownloadComplete
					)
					.addParameter("id", storedFile.id)
					.execute()
				closeableTransaction.setTransactionSuccessful()
			}
		}
	}
}
