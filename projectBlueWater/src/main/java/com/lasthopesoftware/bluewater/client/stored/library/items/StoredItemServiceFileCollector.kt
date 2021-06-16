package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem.ItemType
import com.lasthopesoftware.bluewater.client.stored.library.sync.CollectServiceFilesForSync
import com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.Companion.forward
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.RejectionProxy
import com.namehillsoftware.handoff.promises.propagation.ResolutionProxy
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.util.concurrent.CancellationException

class StoredItemServiceFileCollector(
	private val storedItemAccess: IStoredItemAccess,
	private val fileProvider: ProvideLibraryFiles,
	private val fileListParameters: IFileListParameterProvider) : CollectServiceFilesForSync {

	override fun promiseServiceFilesToSync(libraryId: LibraryId): Promise<Collection<ServiceFile>> {
		return Promise { serviceFileMessenger ->
			val cancellationProxy = CancellationProxy()
			serviceFileMessenger.cancellationRequested(cancellationProxy)

			val promisedStoredItems = storedItemAccess.promiseStoredItems(libraryId)
			cancellationProxy.doCancel(promisedStoredItems)

			val promisedServiceFileLists = promisedStoredItems
				.eventually { storedItems ->
					if (cancellationProxy.isCancelled) Promise(CancellationException())
					else Promise.whenAll(storedItems
						.toList()
						.map { storedItem -> promiseServiceFiles(libraryId, storedItem, cancellationProxy) })
				}
			cancellationProxy.doCancel(promisedServiceFileLists)

			promisedServiceFileLists
				.then<Collection<ServiceFile>> { serviceFiles -> serviceFiles.flatten() }
				.then(ResolutionProxy(serviceFileMessenger), RejectionProxy(serviceFileMessenger))
		}
	}

	private fun promiseServiceFiles(libraryId: LibraryId, storedItem: StoredItem, cancellationProxy: CancellationProxy): Promise<Collection<ServiceFile>> {
		return when (storedItem.itemType) {
			ItemType.ITEM -> promiseServiceFiles(libraryId, Item(storedItem.serviceId), cancellationProxy)
			ItemType.PLAYLIST -> promiseServiceFiles(libraryId, Playlist(storedItem.serviceId), cancellationProxy)
			else -> Promise(emptyList())
		}
	}

	private fun promiseServiceFiles(libraryId: LibraryId, item: Item, cancellationProxy: CancellationProxy): Promise<Collection<ServiceFile>> {
		val parameters = fileListParameters.getFileListParameters(item)
		val serviceFilesPromise = fileProvider.promiseFiles(libraryId, FileListParameters.Options.None, *parameters)
		cancellationProxy.doCancel(serviceFilesPromise)
		return serviceFilesPromise.then(forward(), ExceptionHandler(libraryId, item, storedItemAccess))
	}

	private fun promiseServiceFiles(libraryId: LibraryId, playlist: Playlist, cancellationProxy: CancellationProxy): Promise<Collection<ServiceFile>> {
		val parameters = fileListParameters.getFileListParameters(playlist)
		val serviceFilesPromise = fileProvider.promiseFiles(libraryId, FileListParameters.Options.None, *parameters)
		cancellationProxy.doCancel(serviceFilesPromise)
		return serviceFilesPromise.then(forward(), ExceptionHandler(libraryId, playlist, storedItemAccess))
	}

	private class ExceptionHandler(private val libraryId: LibraryId, private val item: IItem, private val storedItemAccess: IStoredItemAccess) : ImmediateResponse<Throwable, Collection<ServiceFile>> {

		override fun respond(e: Throwable): List<ServiceFile> {
			if (e is FileNotFoundException) {
				logger.warn("The item " + item.key + " was not found, disabling sync for item")
				storedItemAccess.toggleSync(libraryId, item, false)
				return emptyList()
			}
			throw e
		}
	}

	companion object {
		private val logger = LoggerFactory.getLogger(StoredItemServiceFileCollector::class.java)
	}

}
