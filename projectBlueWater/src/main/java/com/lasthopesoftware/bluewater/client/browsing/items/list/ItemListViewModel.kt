package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.itemId
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.HiddenListItemMenu
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.items.menu.ActivityLaunching
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
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
	private val sendItemMenuMessages: SendTypedMessages<ItemListMenuMessage>
) : ViewModel() {

	private val activityLaunchingReceiver = messageBus.registerReceiver { event : ActivityLaunching ->
		mutableIsLoaded.value = event != ActivityLaunching.HALTED // Only show the item list view again when launching error'ed for some reason
	}
	private val mutableItems = MutableStateFlow(emptyList<ChildItemViewModel>())
	private val mutableIsLoaded = MutableStateFlow(true)
	private val mutableIsSynced = MutableStateFlow(false)
	private val mutableItemValue = MutableStateFlow("")

	var loadedItem: IItem? = null
	var loadedLibraryId: LibraryId? = null

	val itemValue = mutableItemValue.asStateFlow()
	val isSynced = mutableIsSynced.asStateFlow()
	val items = mutableItems.asStateFlow()
	val isLoaded = mutableIsLoaded.asStateFlow()

	override fun onCleared() {
		activityLaunchingReceiver.close()
	}

	fun loadItem(item: Item): Promise<Unit> {
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
								mutableItems.value = items.map { item ->
									ChildItemViewModel(item)
								}
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

	inner class ChildItemViewModel internal constructor(val item: IItem) : HiddenListItemMenu {
		private val mutableIsSynced = MutableStateFlow(false)
		private val mutableIsMenuShown = MutableStateFlow(false)

		val isSynced = mutableIsSynced.asStateFlow()
		override val isMenuShown = mutableIsMenuShown.asStateFlow()

		fun play() =
			loadedLibraryId
				?.let { libraryId ->
					itemStringListProvider
						.promiseFileStringList(libraryId, ItemId(item.key), FileListParameters.Options.None)
						.then(controlNowPlaying::startPlaylist)
				}
				.keepPromise(Unit)

		fun playShuffled() =
			loadedLibraryId
				?.let { libraryId ->
					itemStringListProvider
						.promiseFileStringList(libraryId, ItemId(item.key), FileListParameters.Options.Shuffled)
						.then(controlNowPlaying::startPlaylist)
				}
				.keepPromise(Unit)

		fun toggleSync(): Promise<Unit> = loadedLibraryId
			?.let { libraryId ->
				storedItemAccess
					.toggleSync(libraryId, item.itemId)
					.then { mutableIsSynced.value = it }
			}
			.keepPromise(Unit)

		override fun showMenu(): Boolean {
			if (!mutableIsMenuShown.compareAndSet(expect = false, update = true)) return false

			loadedLibraryId
				?.let { libraryId ->
					storedItemAccess
						.isItemMarkedForSync(libraryId, item.itemId)
						.then { mutableIsSynced.value = it }
				}

			sendItemMenuMessages.sendMessage(ItemListMenuMessage.MenuShown(this))

			return true
		}

		override fun hideMenu(): Boolean =
			mutableIsMenuShown.compareAndSet(expect = true, update = false).also {
				if (it) sendItemMenuMessages.sendMessage(ItemListMenuMessage.MenuHidden(this))
			}
	}
}
