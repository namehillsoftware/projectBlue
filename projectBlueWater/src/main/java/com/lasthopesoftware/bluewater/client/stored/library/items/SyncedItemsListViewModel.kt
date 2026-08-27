package com.lasthopesoftware.bluewater.client.stored.library.items

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.observables.MutableInteractionState
import com.namehillsoftware.handoff.promises.Promise

class SyncedItemsListViewModel(
	private val storedItemAccess: AccessStoredItems
) : ViewModel(), TrackLoadedViewState {

	companion object {
		private val viewableItemTypes = setOf(StoredItem.ItemType.ITEM, StoredItem.ItemType.PLAYLIST)
	}

	private val mutableStoredItems = MutableInteractionState(emptyList<StoredItem>())
	private val mutableIsLoading = MutableInteractionState(false)

	override val isLoading = mutableIsLoading.asInteractionState()
	val storedItems = mutableStoredItems.asInteractionState()

	fun loadItems(libraryId: LibraryId): Promise<Unit> {
		mutableIsLoading.value = true
		return storedItemAccess
			.promiseStoredItems(libraryId)
			.then { items ->
				mutableStoredItems.value = items.filter {
					viewableItemTypes.contains(it.itemType)
				}
			}
			.must { _ -> mutableIsLoading.value = false }
	}
}
