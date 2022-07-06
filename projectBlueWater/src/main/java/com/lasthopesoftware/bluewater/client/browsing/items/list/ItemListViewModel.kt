package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.items.menu.ActivityLaunching
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ItemListViewModel(
	private val selectedLibraryId: ProvideSelectedLibraryId,
	private val itemProvider: ProvideItems,
	messageBus: RegisterForApplicationMessages,
	private val storedItemAccess: AccessStoredItems,
	private val itemStringListProvider: ProvideFileStringListForItem,
	private val controlNowPlaying: ControlPlaybackService,
) : ViewModel(), (ActivityLaunching) -> Unit {

	private var loadedItem: IItem? = null
	private val receiver = messageBus.registerReceiver(this)
	private val mutableItems = MutableStateFlow(emptyList<ChildItemViewModel>())
	private val mutableIsLoaded = MutableStateFlow(true)
	private val mutableIsSynced = MutableStateFlow(false)
	private val mutableItemValue = MutableStateFlow("")

	private var loadedLibraryId: LibraryId? = null

	val itemValue = mutableItemValue.asStateFlow()
	val isSynced = mutableIsSynced.asStateFlow()

	val items = mutableItems.asStateFlow()
	val isLoaded = mutableIsLoaded.asStateFlow()

	override fun invoke(event: ActivityLaunching) {
		mutableIsLoaded.value = event != ActivityLaunching.HALTED // Only show the item list view again when launching error'ed for some reason
	}

	override fun onCleared() {
		receiver.close()
	}

	fun loadItems(item: Item): Promise<Unit> {
		mutableIsLoaded.value = false
		mutableItemValue.value = item.value
		return selectedLibraryId.selectedLibraryId
			.eventually { libraryId ->
				loadedLibraryId = libraryId
				libraryId
					?.let {
						val itemUpdate = itemProvider
							.promiseItems(it, item.itemId)
							.then { items ->
								mutableItems.value = items.map(::ChildItemViewModel)
							}

						val promisedSyncUpdate = storedItemAccess
							.isItemMarkedForSync(libraryId, item)
							.then { isSynced ->
								mutableIsSynced.value = isSynced
							}

						Promise.whenAll(itemUpdate, promisedSyncUpdate).unitResponse()
					}
					.keepPromise(Unit)
			}
			.then {
				loadedItem = item
				mutableIsLoaded.value = true
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

	inner class ChildItemViewModel(val item: IItem) {
		private val mutableIsSynced = MutableStateFlow(false)

		val isSynced = mutableIsSynced.asStateFlow()

		fun play() =
			loadedLibraryId
				?.let { libraryId ->
					itemStringListProvider
						.promiseFileStringList(libraryId, ItemId(item.key), FileListParameters.Options.None)
						.then { controlNowPlaying.startPlaylist(it) }
				}
				.keepPromise(Unit)

		fun playShuffled() =
			loadedLibraryId
				?.let { libraryId ->
					itemStringListProvider
						.promiseFileStringList(libraryId, ItemId(item.key), FileListParameters.Options.Shuffled)
						.then { controlNowPlaying.startPlaylist(it) }
				}
				.keepPromise(Unit)

		fun toggleSync(): Promise<Unit> = loadedLibraryId
			?.let { libraryId ->
				item.let { (it as? Item)?.playlistId ?: ItemId(it.key) }.let { key ->
					val isSynced = !mutableIsSynced.value
					storedItemAccess
						.toggleSync(libraryId, key, isSynced)
						.then { mutableIsSynced.value = isSynced }
				}
			}
			.keepPromise(Unit)
	}
}
