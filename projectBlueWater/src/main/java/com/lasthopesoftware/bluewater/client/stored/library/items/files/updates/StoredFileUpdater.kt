package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess.Companion.storedFileAccessExecutor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GetStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.client.stored.library.sync.LookupSyncDirectory
import com.lasthopesoftware.bluewater.repository.InsertBuilder.Companion.fromTable
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.UpdateBuilder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.lazyj.Lazy
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class StoredFileUpdater(
	private val context: Context,
	private val mediaFileUriProvider: MediaFileUriProvider,
	private val mediaFileIdProvider: MediaFileIdProvider,
	private val storedFiles: GetStoredFiles,
	private val libraryProvider: ILibraryProvider,
	private val libraryFileProperties: ProvideLibraryFileProperties,
	private val lookupSyncDirectory: LookupSyncDirectory
) : UpdateStoredFiles {

	override fun promiseStoredFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile?> {
		val promisedLibrary = libraryProvider.getLibrary(libraryId)
		return storedFiles.promiseStoredFile(libraryId, serviceFile)
			.eventually { storedFile ->
				storedFile
					?.toPromise()
					?: QueuedPromise(MessageWriter {
						RepositoryAccessHelper(context).use { repositoryAccessHelper ->
							logger.info("Stored file was not found for " + serviceFile.key + ", creating file")
							createStoredFile(libraryId, repositoryAccessHelper, serviceFile)
						}
					}, storedFileAccessExecutor())
						.eventually { storedFiles.promiseStoredFile(libraryId, serviceFile) }
			}
			.eventually { storedFile ->
				promisedLibrary.eventually { library ->
					if (storedFile.path != null || !library!!.isUsingExistingFiles) return@eventually Promise(storedFile)
					mediaFileUriProvider
						.promiseFileUri(serviceFile)
						.eventually { localUri ->
							localUri?.let { u ->
								storedFile.path = u.path
								storedFile.setIsDownloadComplete(true)
								storedFile.setIsOwner(false)
								mediaFileIdProvider
									.getMediaId(libraryId, serviceFile)
									.then { mediaId ->
										storedFile.storedMediaId = mediaId
										storedFile
									}
							} ?: Promise(storedFile)
						}
				}
			}
			.eventually { storedFile ->
				if (storedFile.path != null) Promise(storedFile) else libraryFileProperties
					.promiseFileProperties(libraryId, serviceFile)
					.eventually { fileProperties ->
						lookupSyncDirectory.promiseSyncDirectory(libraryId)
							.then { syncDir ->
								var fullPath = syncDir?.path ?: return@then null
								var artist = fileProperties[KnownFileProperties.ALBUM_ARTIST]
								if (artist == null) artist = fileProperties[KnownFileProperties.ARTIST]
								if (artist != null) fullPath = FilenameUtils.concat(
									fullPath,
									replaceReservedCharsAndPath(artist.trim { it <= ' ' })
								)
								val album = fileProperties[KnownFileProperties.ALBUM]
								if (album != null) fullPath = FilenameUtils.concat(
									fullPath,
									replaceReservedCharsAndPath(album.trim { it <= ' ' })
								)
								val fileName = fileProperties[KnownFileProperties.FILENAME]?.let { f ->
									var lastPathIndex = f.lastIndexOf('\\')
									if (lastPathIndex < 0) lastPathIndex = f.lastIndexOf('/')
									if (lastPathIndex > -1) {
										var fileName = f.substring(lastPathIndex + 1)
										val extensionIndex = fileName.lastIndexOf('.')
										if (extensionIndex > -1) fileName =
											fileName.substring(0, extensionIndex + 1) + "mp3"
										fileName
									} else {
										f
									}
								}
								fullPath =
									FilenameUtils.concat(fullPath, fileName).trim { it <= ' ' }
								storedFile.path = fullPath
								storedFile
							}
					}
			}
			.eventually { storedFile ->
				storedFile?.let { sf ->
					QueuedPromise(MessageWriter {
						RepositoryAccessHelper(context).use { repositoryAccessHelper ->
							updateStoredFile(repositoryAccessHelper, sf)
							sf
						}
					}, storedFileAccessExecutor())
				} ?: Promise.empty()
			}
	}

	private fun createStoredFile(
		libraryId: LibraryId,
		repositoryAccessHelper: RepositoryAccessHelper,
		serviceFile: ServiceFile
	) {
		repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
			repositoryAccessHelper
				.mapSql(insertSql.getObject())
				.addParameter(StoredFileEntityInformation.serviceIdColumnName, serviceFile.key)
				.addParameter(StoredFileEntityInformation.libraryIdColumnName, libraryId.id)
				.addParameter(StoredFileEntityInformation.isOwnerColumnName, true)
				.execute()
			closeableTransaction.setTransactionSuccessful()
		}
	}

	companion object {
		private val logger = LoggerFactory.getLogger(StoredFileUpdater::class.java)
		private val insertSql = Lazy {
			fromTable(StoredFileEntityInformation.tableName)
				.addColumn(StoredFileEntityInformation.serviceIdColumnName)
				.addColumn(StoredFileEntityInformation.libraryIdColumnName)
				.addColumn(StoredFileEntityInformation.isOwnerColumnName)
				.build()
		}
		private val updateSql = Lazy {
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
		private val reservedCharactersPattern = Lazy { Pattern.compile("[|?*<\":>+\\[\\]'/]") }
		private fun replaceReservedCharsAndPath(path: String): String {
			return reservedCharactersPattern.getObject().matcher(path).replaceAll("_")
		}

		private fun updateStoredFile(
			repositoryAccessHelper: RepositoryAccessHelper,
			storedFile: StoredFile
		) {
			repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
				repositoryAccessHelper
					.mapSql(updateSql.getObject())
					.addParameter(
						StoredFileEntityInformation.serviceIdColumnName,
						storedFile.serviceId
					)
					.addParameter(
						StoredFileEntityInformation.storedMediaIdColumnName,
						storedFile.storedMediaId
					)
					.addParameter(StoredFileEntityInformation.pathColumnName, storedFile.path)
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, storedFile.isOwner)
					.addParameter(
						StoredFileEntityInformation.isDownloadCompleteColumnName,
						storedFile.isDownloadComplete
					)
					.addParameter("id", storedFile.id)
					.execute()
				closeableTransaction.setTransactionSuccessful()
			}
		}
	}
}
