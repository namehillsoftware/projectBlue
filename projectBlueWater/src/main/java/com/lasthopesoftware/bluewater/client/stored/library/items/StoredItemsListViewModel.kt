package com.lasthopesoftware.bluewater.client.stored.library.items

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.observables.LiftedInteractionState
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.observables.mapNotNull
import com.namehillsoftware.handoff.promises.Promise

class StoredItemsListViewModel(
	private val storedItemAccess: AccessStoredItems
) : ViewModel(), TrackLoadedViewState {

	companion object {
		private val viewableItemTypes = setOf(StoredItem.ItemType.ITEM, StoredItem.ItemType.PLAYLIST)
	}

	private val mutableStoredItems = MutableInteractionState(emptyList<StoredItem>())
	private val mutableIsLoading = MutableInteractionState(false)

	override val isLoading = mutableIsLoading.asInteractionState()
	val items by lazy {
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

	var loadedLibraryId: LibraryId? = null
		private set

	fun loadItems(libraryId: LibraryId): Promise<Unit> {
		mutableIsLoading.value = true
		return storedItemAccess
			.promiseStoredItems(libraryId)
			.then { items ->
				mutableStoredItems.value = items.filter {
					viewableItemTypes.contains(it.itemType)
				}
				loadedLibraryId = libraryId
			}
			.must { _ -> mutableIsLoading.value = false }
	}
}
