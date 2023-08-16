package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.setURI
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.Companion.forward
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class StoredFileUpdater(
	private val storedFileAccess: AccessStoredFiles,
	private val mediaFileUriProvider: MediaFileUriProvider,
	private val libraryProvider: ILibraryProvider,
	private val lookupStoredFilePaths: GetStoredFileUris,
) : UpdateStoredFiles {
	override fun markStoredFileAsDownloaded(storedFile: StoredFile): Promise<StoredFile> {
		storedFile.setIsDownloadComplete(true)
		return storedFileAccess
			.promiseUpdatedStoredFile(storedFile)
			.then(forward()) {
				storedFile.setIsDownloadComplete(false)
			}
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
