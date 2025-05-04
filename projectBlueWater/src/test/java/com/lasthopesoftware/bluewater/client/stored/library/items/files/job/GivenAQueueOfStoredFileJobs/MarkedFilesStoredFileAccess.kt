package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.namehillsoftware.handoff.promises.Promise

open class MarkedFilesStoredFilesUpdater : UpdateStoredFiles {
	val storedFilesMarkedAsDownloaded: MutableList<StoredFile> = ArrayList()

    override fun markStoredFileAsDownloaded(storedFile: StoredFile): Promise<StoredFile> {
        storedFilesMarkedAsDownloaded.add(storedFile.setIsDownloadComplete(true))
        return Promise(storedFile)
    }

	override fun promiseStoredFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile> =
		Promise(StoredFile())
}
