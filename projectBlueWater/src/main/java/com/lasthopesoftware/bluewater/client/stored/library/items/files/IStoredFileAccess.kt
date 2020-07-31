package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise

interface IStoredFileAccess {
	fun getStoredFile(storedFileId: Int): Promise<StoredFile?>
	fun getStoredFile(library: Library, serviceFile: ServiceFile): Promise<StoredFile?>
	val downloadingStoredFiles: Promise<List<StoredFile>>
	fun markStoredFileAsDownloaded(storedFile: StoredFile): Promise<StoredFile>
	fun addMediaFile(library: Library, serviceFile: ServiceFile, mediaFileId: Int, filePath: String): Promise<Unit>
	fun pruneStoredFiles(libraryId: LibraryId, serviceFilesToKeep: Set<ServiceFile>): Promise<Unit>
}
