package com.lasthopesoftware.bluewater.client.stored.library.items.files

import android.content.Context
import android.database.SQLException
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation.isDownloadCompleteColumnName
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation.libraryIdColumnName
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation.serviceIdColumnName
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation.tableName
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.fetch
import com.lasthopesoftware.bluewater.repository.fetchFirst
import com.lasthopesoftware.bluewater.repository.insert
import com.lasthopesoftware.bluewater.repository.update
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise

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

	fun promiseNewStoredFile(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile> =
		promiseTableMessage<StoredFile, StoredFile> {
			RepositoryAccessHelper(context).use {
				it.createStoredFile(libraryId, serviceFile)
			}
		}

	private fun getStoredFileTask(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile?> =
		promiseTableMessage<StoredFile?, StoredFile> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.getStoredFile(
					libraryId,
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
			storedFile.setIsDownloadComplete(true)

			try {
				RepositoryAccessHelper(context).update(tableName, storedFile)
			} catch (e: Throwable) {
				logger.warn("An error occurred updating the stored file ${storedFile.id}.", e)
				storedFile.setIsDownloadComplete(false)
				storedFile
			}
		}

	private fun RepositoryAccessHelper.getStoredFile(library: LibraryId, serviceFile: ServiceFile): StoredFile? =
		beginNonExclusiveTransaction().use {
			mapSql(" SELECT *  FROM $tableName WHERE $serviceIdColumnName = @$serviceIdColumnName AND $libraryIdColumnName = @$libraryIdColumnName")
				.addParameter(serviceIdColumnName, serviceFile.key)
				.addParameter(libraryIdColumnName, library.id)
				.fetchFirst()
		}

	private fun getStoredFile(helper: RepositoryAccessHelper, storedFileId: Int): StoredFile? =
		helper.beginNonExclusiveTransaction().use {
			helper
				.mapSql("SELECT * FROM $tableName WHERE id = @id")
				.addParameter("id", storedFileId)
				.fetchFirst()
		}

	private fun RepositoryAccessHelper.createStoredFile(libraryId: LibraryId, serviceFile: ServiceFile): StoredFile =
		insert(tableName, StoredFile(libraryId, serviceFile, null, true))

	override fun deleteStoredFile(storedFile: StoredFile): Promise<Unit> =
		promiseTableMessage<Unit, StoredFile> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				try {
					repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
						repositoryAccessHelper
							.mapSql("DELETE FROM $tableName WHERE id = @id")
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
		private val logger by lazyLogger<StoredFileAccess>()
		private const val selectFromStoredFiles = "SELECT * FROM $tableName"
	}
}
