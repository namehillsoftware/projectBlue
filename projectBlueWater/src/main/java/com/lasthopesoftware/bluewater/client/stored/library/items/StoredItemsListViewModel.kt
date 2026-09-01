package com.lasthopesoftware.bluewater.client.stored.library.items

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.LoadItemData
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewState
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.observables.LiftedInteractionState
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.observables.StaticInteractionState
import com.lasthopesoftware.observables.mapNotNull
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.Promise

class StoredItemsListViewModel(
	private val storedItemAccess: AccessStoredItems,
	stringResources: GetStringResources,
	applicationMessages: RegisterForApplicationMessages,
) : ViewModel(), ItemListViewState, LoadItemData {

	companion object {
		private val viewableItemTypes = setOf(StoredItem.ItemType.ITEM, StoredItem.ItemType.PLAYLIST)
	}

	private val mutableStoredItems = MutableInteractionState(emptyList<StoredItem>())
	private val mutableIsLoading = MutableInteractionState(false)

	override val isLoading = mutableIsLoading.asInteractionState()
	override val itemValue = StaticInteractionState(stringResources.syncedItems)
	override val items by lazy {
		LiftedInteractionState(
			mutableStoredItems.mapNotNull().map { items ->
				items.map {
					if (it.itemType != StoredItem.ItemType.PLAYLIST) Item(it.serviceId, it.itemName)
					else Playlist(it.serviceId, it.itemName)
				}
			},
			emptyList()
		).also(::addCloseable)
	}

	override val loadedItem: IItem? = null

	@Volatile
	override var loadedLibraryId: LibraryId? = null
		private set

	init {
		addCloseable(
			applicationMessages.registerReceiver { e: SyncItemStateChanged ->
				if (e.libraryId == loadedLibraryId)
					loadItem(e.libraryId)
			}
		)
	}

	override fun loadItem(libraryId: LibraryId, item: IItem?): Promise<Unit> {
		mutableIsLoading.value = true
		loadedLibraryId = libraryId
		return storedItemAccess
			.promiseStoredItems(libraryId)
			.then { items ->
				if (loadedLibraryId == libraryId) {
					mutableStoredItems.value = items.filter {
						viewableItemTypes.contains(it.itemType)
					}
				}
			}
			.must { _ -> mutableIsLoading.value = false }
	}

	override fun promiseRefresh(): Promise<Unit> {
		val libraryId = loadedLibraryId ?: return Unit.toPromise()
		return loadItem(libraryId)
	}
}
