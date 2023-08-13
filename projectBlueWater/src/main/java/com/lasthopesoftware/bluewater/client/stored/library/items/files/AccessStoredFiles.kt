package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise

interface AccessStoredFiles {
	fun getStoredFile(storedFileId: Int): Promise<StoredFile?>
	fun getStoredFile(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile?>
	fun promiseAllStoredFiles(libraryId: LibraryId): Promise<Collection<StoredFile>>
	fun promiseDanglingFiles(): Promise<Collection<StoredFile>>
	fun promiseDownloadingFiles(): Promise<List<StoredFile>>
	fun markStoredFileAsDownloaded(storedFile: StoredFile): Promise<StoredFile>
	fun deleteStoredFile(storedFile: StoredFile): Promise<Unit>
}
