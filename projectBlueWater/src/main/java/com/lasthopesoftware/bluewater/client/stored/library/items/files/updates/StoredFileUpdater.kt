package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GetStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.ProvideMediaFileIds
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.repository.InsertBuilder.Companion.fromTable
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.UpdateBuilder
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise

class StoredFileUpdater(
	private val context: Context,
	private val mediaFileUriProvider: MediaFileUriProvider,
	private val mediaFileIdProvider: ProvideMediaFileIds,
	private val storedFiles: GetStoredFiles,
	private val libraryProvider: ILibraryProvider,
	private val lookupStoredFilePaths: GetStoredFilePaths,
	private val externalMediaItemCreator: CreateExternalMediaItem,
) : UpdateStoredFiles {

	companion object {
		private val logger by lazyLogger<StoredFileUpdater>()

		private val insertSql by lazy {
			fromTable(StoredFileEntityInformation.tableName)
				.addColumn(StoredFileEntityInformation.serviceIdColumnName)
				.addColumn(StoredFileEntityInformation.libraryIdColumnName)
				.addColumn(StoredFileEntityInformation.isOwnerColumnName)
				.build()
		}

		private val updateSql by lazy {
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

		private fun RepositoryAccessHelper.updateStoredFile(storedFile: StoredFile) {
			beginTransaction().use { closeableTransaction ->
					mapSql(updateSql)
						.addParameter(StoredFileEntityInformation.serviceIdColumnName, storedFile.serviceId)
						.addParameter(StoredFileEntityInformation.storedMediaIdColumnName, storedFile.storedMediaId)
						.addParameter(StoredFileEntityInformation.pathColumnName, storedFile.path)
						.addParameter(StoredFileEntityInformation.isOwnerColumnName, storedFile.isOwner)
						.addParameter(StoredFileEntityInformation.isDownloadCompleteColumnName, storedFile.isDownloadComplete)
						.addParameter("id", storedFile.id)
						.execute()
				closeableTransaction.setTransactionSuccessful()
			}
		}

		private fun RepositoryAccessHelper.createStoredFile(libraryId: LibraryId, serviceFile: ServiceFile) {
			beginTransaction().use { closeableTransaction ->
				mapSql(insertSql)
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, serviceFile.key)
					.addParameter(StoredFileEntityInformation.libraryIdColumnName, libraryId.id)
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, true)
					.execute()
				closeableTransaction.setTransactionSuccessful()
			}
		}
	}

	override fun promiseStoredFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile> {
		fun storedFileWithMediaId(storedFile: StoredFile): Promise<StoredFile> =
			if (storedFile.storedMediaId > 0) storedFile.toPromise()
			else externalMediaItemCreator.promiseCreatedItem(libraryId, serviceFile).then { it?.let(storedFile::setStoredMediaId) }

		fun storedFileWithFilePath(storedFile: StoredFile): Promise<StoredFile> =
			if (storedFile.path != null) storedFile.toPromise()
			else lookupStoredFilePaths.promiseStoredFilePath(libraryId, serviceFile).then(storedFile::setPath)

		val promisedLibrary = libraryProvider.promiseLibrary(libraryId)
		return storedFiles.promiseStoredFile(libraryId, serviceFile)
			.eventually { storedFile ->
				storedFile?.toPromise()
					?: promiseTableMessage<Unit, StoredFile> {
							RepositoryAccessHelper(context).use { repositoryAccessHelper ->
								logger.info("Stored file was not found for " + serviceFile.key + ", creating file")
								repositoryAccessHelper.createStoredFile(libraryId, serviceFile)
							}
						}
						.eventually { storedFiles.promiseStoredFile(libraryId, serviceFile) }
			}
			.eventually { storedFile ->
				promisedLibrary
					.eventually { library ->
						library
							?.takeIf { it.isUsingExistingFiles && storedFile.path == null }
							?.let {
								mediaFileUriProvider
									.promiseUri(libraryId, serviceFile)
									.eventually { localUri ->
										localUri
											?.let { u ->
												storedFile.setPath(u.path)
												storedFile.setIsDownloadComplete(true)
												storedFile.setIsOwner(false)
												mediaFileIdProvider
													.getMediaId(libraryId, serviceFile)
													.then(storedFile::setStoredMediaId)
											}
											.keepPromise(storedFile)
									}
							}
							.keepPromise(storedFile)
							.eventually { storedFile ->
								if (library?.syncedFileLocation == Library.SyncedFileLocation.EXTERNAL) storedFileWithMediaId(storedFile)
								else storedFileWithFilePath(storedFile)
							}
					}
			}
			.eventually { sf ->
				promiseTableMessage<StoredFile, StoredFile> {
					RepositoryAccessHelper(context).use { repositoryAccessHelper ->
						repositoryAccessHelper.updateStoredFile(sf)
						sf
					}
				}
			}
	}
}
