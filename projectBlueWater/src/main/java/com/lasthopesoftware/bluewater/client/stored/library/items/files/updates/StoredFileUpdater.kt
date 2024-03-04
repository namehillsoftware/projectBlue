package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.HaveExternalContent
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.setURI
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.uri.IoCommon
import com.namehillsoftware.handoff.promises.Promise
import java.net.URI

class StoredFileUpdater(
	private val storedFileAccess: AccessStoredFiles,
	private val mediaFileUriProvider: MediaFileUriProvider,
	private val libraryProvider: ILibraryProvider,
	private val lookupStoredFilePaths: GetStoredFileUris,
	private val externalContent: HaveExternalContent,
) : UpdateStoredFiles {
	override fun markStoredFileAsDownloaded(storedFile: StoredFile): Promise<StoredFile> {
		storedFile.setIsDownloadComplete(true)
		return storedFileAccess
			.promiseUpdatedStoredFile(storedFile)
			.eventually(
				{ sf ->
					sf.uri
						?.let(::URI)
						?.takeIf { it.scheme == IoCommon.contentUriScheme }
						?.let { externalContent.markContentAsNotPending(it).then {  _ -> sf } }
						.keepPromise(sf)
				},
				{
					storedFile.setIsDownloadComplete(false).toPromise()
				}
			)
	}

	override fun promiseStoredFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile> {
		fun storedFileWithUri(storedFile: StoredFile): Promise<StoredFile> =
			storedFile
				.takeIf { it.uri.isNullOrEmpty() }
				?.let { lookupStoredFilePaths.promiseStoredFileUri(libraryId, serviceFile).then(it::setURI) }
				.keepPromise(storedFile)

		val promisedLibrary = libraryProvider.promiseLibrary(libraryId)
		return storedFileAccess.promiseStoredFile(libraryId, serviceFile)
			.eventually { storedFile ->
				storedFile
					?.toPromise()
					?: storedFileAccess.promiseNewStoredFile(libraryId, serviceFile)
			}
			.eventually { storedFile ->
				promisedLibrary
					.eventually { library ->
						library
							?.takeIf { it.isUsingExistingFiles && storedFile.uri == null }
							?.let {
								mediaFileUriProvider
									.promiseUri(libraryId, serviceFile)
									.then { localUri ->
										storedFile
											.apply {
												if (localUri != null) {
													setUri(localUri.toString())
													setIsDownloadComplete(true)
													setIsOwner(false)
												}
											}
									}
							}
							.keepPromise(storedFile)
					}
			}
			.eventually(::storedFileWithUri)
			.eventually(storedFileAccess::promiseUpdatedStoredFile)
	}
}
