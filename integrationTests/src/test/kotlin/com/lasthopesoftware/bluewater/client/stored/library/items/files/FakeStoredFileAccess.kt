package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class FakeStoredFileAccess(vararg initialStoredFiles: Pair<LibraryId, ServiceFile>) : AccessStoredFiles {
	private val storedFileCounter = AtomicInteger(0)

	val storedFiles = ConcurrentHashMap<Int, StoredFile>()

	init {
	    for ((libraryId, serviceFile) in initialStoredFiles)
			promiseNewStoredFile(libraryId, serviceFile)
	}

	val storedFilesMarkedAsDownloaded = storedFiles.values.filter { sf -> sf.isDownloadComplete }

    override fun promiseStoredFile(storedFileId: Int): Promise<StoredFile?> =
		storedFiles.values.firstOrNull { sf -> sf.id == storedFileId }.toPromise()

    override fun promiseStoredFile(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile?> =
		storedFiles.values.firstOrNull { sf -> sf.libraryId == libraryId.id && sf.serviceId == serviceFile.key }.toPromise()
	override fun promiseAllStoredFiles(libraryId: LibraryId): Promise<Collection<StoredFile>> =
		storedFiles.values.filter { it.libraryId == libraryId.id }.toPromise()

	override fun promiseDownloadingFiles(): Promise<List<StoredFile>> =
		storedFiles.values.filter { sf -> !sf.isDownloadComplete }.toPromise()

	override fun deleteStoredFile(storedFile: StoredFile): Promise<Unit> {
		storedFiles.remove(storedFile.id)
		return Unit.toPromise()
	}

	override fun promiseNewStoredFile(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile> {
		val id = storedFileCounter.getAndIncrement()
		val storedFile = StoredFile(libraryId, serviceFile, null, true)
		storedFiles[id] = storedFile.setId(id)

		return storedFile.toPromise()
	}

	override fun promiseUpdatedStoredFile(storedFile: StoredFile): Promise<StoredFile> {
		storedFiles[storedFile.id] = storedFile
		return storedFile.toPromise()
	}

	override fun promiseDanglingFiles(): Promise<Collection<StoredFile>> {
        return Promise.empty()
    }
}
