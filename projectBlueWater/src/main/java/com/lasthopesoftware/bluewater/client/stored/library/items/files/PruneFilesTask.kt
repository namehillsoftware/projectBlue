package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.io.File

internal class PruneFilesTask(private val storedFileAccess: StoredFileAccess, serviceFilesToKeep: Collection<ServiceFile>) : PromisedResponse<Collection<StoredFile>, Unit> {
	private val serviceIdsToKeep by lazy { serviceFilesToKeep.map { obj -> obj.key } }

	override fun promiseResponse(allStoredFiles: Collection<StoredFile>): Promise<Unit> =
		QueuedPromise(CancellableMessageWriter { ct ->
			for (storedFile in allStoredFiles) {
				if (ct.isCancelled) break

				val filePath = storedFile.path
				// It doesn't make sense to create a stored serviceFile without a serviceFile path
				if (filePath == null) {
					storedFileAccess.deleteStoredFile(storedFile)
					continue
				}

				if (ct.isCancelled) break

				val systemFile = File(filePath)

				// Remove files that are marked as downloaded but the file doesn't actually exist
				if (storedFile.isDownloadComplete && !systemFile.exists()) {
					storedFileAccess.deleteStoredFile(storedFile)
					continue
				}

				if (!storedFile.isOwner) continue
				if (serviceIdsToKeep.contains(storedFile.serviceId)) continue
				if (ct.isCancelled) break

				storedFileAccess.deleteStoredFile(storedFile)
				if (!systemFile.delete()) continue

				var directoryToDelete = systemFile.parentFile
				while (directoryToDelete != null) {
					if (ct.isCancelled) break
					val childList = directoryToDelete.list()
					if (childList != null && childList.isNotEmpty()) break
					if (!directoryToDelete.delete()) break
					directoryToDelete = directoryToDelete.parentFile
				}
			}
		}, ThreadPools.io)
}
