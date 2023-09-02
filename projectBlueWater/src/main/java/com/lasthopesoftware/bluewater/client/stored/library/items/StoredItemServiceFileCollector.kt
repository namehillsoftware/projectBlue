package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.IFileListParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem.ItemType
import com.lasthopesoftware.bluewater.client.stored.library.sync.CollectServiceFilesForSync
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.Companion.forward
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.io.FileNotFoundException
import java.util.concurrent.CancellationException

class StoredItemServiceFileCollector(
	private val storedItemAccess: AccessStoredItems,
	private val fileProvider: ProvideLibraryFiles,
	private val fileListParameters: IFileListParameterProvider
) : CollectServiceFilesForSync {

	companion object {
		private val logger by lazyLogger<StoredItemServiceFileCollector>()
	}

	override fun promiseServiceFilesToSync(libraryId: LibraryId): Promise<Collection<ServiceFile>> {
		return CancellableProxyPromise { cancellationProxy ->
			val promisedStoredItems = storedItemAccess.promiseStoredItems(libraryId)
			cancellationProxy.doCancel(promisedStoredItems)

			val promisedServiceFileLists = promisedStoredItems
				.eventually { storedItems ->
					if (cancellationProxy.isCancelled) Promise(CancellationException())
					else Promise
						.whenAll(storedItems.map { storedItem -> promiseServiceFiles(libraryId, storedItem, cancellationProxy) })
				}
			cancellationProxy.doCancel(promisedServiceFileLists)

			promisedServiceFileLists
				.eventually { serviceFiles ->
					QueuedPromise(
						MessageWriter{ serviceFiles.asSequence().flatten().toSet() },
						ThreadPools.compute
					)
				}
		}
	}

	private fun promiseServiceFiles(libraryId: LibraryId, storedItem: StoredItem, cancellationProxy: CancellationProxy): Promise<Collection<ServiceFile>> {
		return when (storedItem.itemType) {
			ItemType.ITEM -> promiseServiceFiles(libraryId, ItemId(storedItem.serviceId), cancellationProxy)
			ItemType.PLAYLIST -> promiseServiceFiles(libraryId, PlaylistId(storedItem.serviceId), cancellationProxy)
			else -> Promise(emptyList())
		}
	}

	private fun promiseServiceFiles(libraryId: LibraryId, item: ItemId, cancellationProxy: CancellationProxy): Promise<Collection<ServiceFile>> {
		val parameters = fileListParameters.getFileListParameters(item)
		val serviceFilesPromise = fileProvider.promiseFiles(libraryId, FileListParameters.Options.None, *parameters)
		cancellationProxy.doCancel(serviceFilesPromise)
		return serviceFilesPromise.then(forward(), ExceptionHandler(libraryId, item, storedItemAccess))
	}

	private fun promiseServiceFiles(libraryId: LibraryId, playlist: PlaylistId, cancellationProxy: CancellationProxy): Promise<Collection<ServiceFile>> {
		val parameters = fileListParameters.getFileListParameters(playlist)
		val serviceFilesPromise = fileProvider.promiseFiles(libraryId, FileListParameters.Options.None, *parameters)
		cancellationProxy.doCancel(serviceFilesPromise)
		return serviceFilesPromise.then(forward(), ExceptionHandler(libraryId, playlist, storedItemAccess))
	}

	private class ExceptionHandler(private val libraryId: LibraryId, private val item: KeyedIdentifier, private val storedItemAccess: AccessStoredItems) : ImmediateResponse<Throwable, Collection<ServiceFile>> {

		override fun respond(e: Throwable): List<ServiceFile> {
			if (e is FileNotFoundException) {
				logger.warn("The item ${item.id} was not found, disabling sync for item")
				storedItemAccess.toggleSync(libraryId, item, false)
				return emptyList()
			}
			throw e
		}
	}
}
