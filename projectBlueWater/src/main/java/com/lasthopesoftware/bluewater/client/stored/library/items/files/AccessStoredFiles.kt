package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise

interface AccessStoredFiles {
	fun getStoredFile(storedFileId: Int): Promise<StoredFile?>
	fun getStoredFile(library: Library, serviceFile: ServiceFile): Promise<StoredFile?>
	fun promiseDanglingFiles(): Promise<Collection<StoredFile>>
	fun promiseDownloadingFiles(): Promise<List<StoredFile>>
	fun markStoredFileAsDownloaded(storedFile: StoredFile): Promise<StoredFile>
	fun addMediaFile(library: Library, serviceFile: ServiceFile, mediaFileId: Int, filePath: String): Promise<Unit>
	fun deleteStoredFile(storedFile: StoredFile): Promise<Unit>
}
