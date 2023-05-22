package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise

interface AccessStoredFiles {
	fun getStoredFile(storedFileId: Int): Promise<StoredFile?>
	fun getStoredFile(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile?>
	fun promiseDanglingFiles(): Promise<Collection<StoredFile>>
	fun promiseDownloadingFiles(): Promise<List<StoredFile>>
	fun markStoredFileAsDownloaded(storedFile: StoredFile): Promise<StoredFile>
	fun addMediaFile(libraryId: LibraryId, serviceFile: ServiceFile, mediaFileId: Int, filePath: String): Promise<Unit>
	fun deleteStoredFile(storedFile: StoredFile): Promise<Unit>
}
