package com.lasthopesoftware.bluewater.client.browsing.files.list

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FileListViewModel(
	private val itemFileProvider: ProvideItemFiles,
	private val storedItemAccess: AccessStoredItems,
) : ViewModel(), TrackLoadedViewState {

	private val mutableIsLoading = MutableStateFlow(false)
	private val mutableFiles = MutableStateFlow(emptyList<ServiceFile>())
	private val mutableItemValue = MutableStateFlow("")
	private val mutableIsSynced = MutableStateFlow(false)

	private var loadedItem: IItem? = null
	private var loadedLibraryId: LibraryId? = null

	override val isLoading = mutableIsLoading.asStateFlow()
	val files = mutableFiles.asStateFlow()
	val itemValue = mutableItemValue.asStateFlow()
	val isSynced = mutableIsSynced.asStateFlow()

	fun loadItem(libraryId: LibraryId, item: IItem? = null): Promise<Unit> {
		mutableIsLoading.value = libraryId != loadedLibraryId || item != loadedItem
		mutableItemValue.value = item?.value ?: ""
		mutableIsSynced.value = false
		loadedLibraryId = libraryId

		val promisedFilesUpdate = itemFileProvider
			.promiseFiles(libraryId, item?.key?.let(::ItemId), FileListParameters.Options.None)
			.then { f -> mutableFiles.value = f }

		val promisedSyncUpdate = item
			?.let {
				storedItemAccess
					.isItemMarkedForSync(libraryId, it)
					.then { isSynced ->
						mutableIsSynced.value = isSynced
					}
			}
			.keepPromise()

		return Promise.whenAll(promisedFilesUpdate, promisedSyncUpdate)
			.then {
				loadedItem = item
			}
			.must {
				mutableIsLoading.value = false
			}
	}

	fun toggleSync(): Promise<Unit> = loadedLibraryId
		?.let { libraryId ->
			loadedItem?.let { (it as? Item)?.playlistId ?: ItemId(it.key) }?.let { key ->
				val isSynced = !mutableIsSynced.value
				storedItemAccess
					.toggleSync(libraryId, key, isSynced)
					.then { mutableIsSynced.value = isSynced }
			}
		}
		.keepPromise(Unit)
}
