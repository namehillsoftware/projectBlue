package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise

interface AccessStoredFiles {
	fun promiseStoredFile(storedFileId: Int): Promise<StoredFile?>
	fun promiseStoredFile(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile?>
	fun promiseAllStoredFiles(libraryId: LibraryId): Promise<Collection<StoredFile>>
	fun promiseDanglingFiles(): Promise<Collection<StoredFile>>
	fun promiseDownloadingFiles(): Promise<List<StoredFile>>
	fun deleteStoredFile(storedFile: StoredFile): Promise<Unit>
	fun promiseNewStoredFile(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile>
	fun promiseUpdatedStoredFile(storedFile: StoredFile): Promise<StoredFile>
}
