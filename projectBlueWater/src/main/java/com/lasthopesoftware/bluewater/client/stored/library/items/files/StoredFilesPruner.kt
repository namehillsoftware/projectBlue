package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.sync.CollectServiceFilesForSync
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.IoCommon
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.io.File
import java.net.URI

class StoredFilesPruner(
	private val serviceFilesToSyncCollector: CollectServiceFilesForSync,
	private val storedFileAccess: AccessStoredFiles
) :
	PruneStoredFiles
{
	override fun pruneDanglingFiles(): Promise<Unit> =
		storedFileAccess
			.promiseDanglingFiles()
			.eventually(PruneFilesTask(emptyList()))

	override fun pruneStoredFiles(libraryId: LibraryId): Promise<Unit> {
		val promisedStoredFiles = storedFileAccess.promiseAllStoredFiles(libraryId)
		val promisedServiceFilesToKeep = serviceFilesToSyncCollector.promiseServiceFilesToSync(libraryId)
		return promisedServiceFilesToKeep
			.eventually { serviceFilesToKeep ->
				promisedStoredFiles.eventually((PruneFilesTask(serviceFilesToKeep)))
			}
	}

	inner class PruneFilesTask(serviceFilesToKeep: Collection<ServiceFile>) :
		PromisedResponse<Collection<StoredFile>, Unit> {
		private val serviceIdsToKeep by lazy { serviceFilesToKeep.map { obj -> obj.key }.toSet() }

		override fun promiseResponse(allStoredFiles: Collection<StoredFile>): Promise<Unit> =
			QueuedPromise(CancellableMessageWriter { ct ->
				for (storedFile in allStoredFiles) {
					if (ct.isCancelled) break

					val uriString = storedFile.uri
					// It doesn't make sense to create a stored serviceFile without a serviceFile path
					if (uriString == null) {
						storedFileAccess.deleteStoredFile(storedFile)
						continue
					}

					val uri = URI(uriString)
					if (uri.scheme != IoCommon.fileUriScheme) continue

					if (ct.isCancelled) break

					val systemFile = File(uri)

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
}
