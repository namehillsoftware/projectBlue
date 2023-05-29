package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class MarkedFilesStoredFileAccess : AccessStoredFiles {
	val storedFilesMarkedAsDownloaded: MutableList<StoredFile> = ArrayList()

    override fun getStoredFile(storedFileId: Int): Promise<StoredFile?> {
        return Promise.empty()
    }

    override fun getStoredFile(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile?> {
        return Promise.empty()
    }

	override fun promiseDownloadingFiles(): Promise<List<StoredFile>> = Promise(emptyList())

    override fun markStoredFileAsDownloaded(storedFile: StoredFile): Promise<StoredFile> {
        storedFilesMarkedAsDownloaded.add(storedFile)
        return Promise(storedFile)
    }

    override fun addMediaFile(libraryId: LibraryId, serviceFile: ServiceFile, mediaFileId: Int, filePath: String): Promise<Unit> {
        return Unit.toPromise()
    }

	override fun deleteStoredFile(storedFile: StoredFile): Promise<Unit> {
		return Unit.toPromise()
	}

	override fun promiseDanglingFiles(): Promise<Collection<StoredFile>> {
        return Promise.empty()
    }
}
