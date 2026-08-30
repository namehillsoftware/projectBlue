package com.lasthopesoftware.bluewater.client.browsing.items.list

import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.files.list.LoadedLibraryState
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.LoadItemData
import com.lasthopesoftware.observables.InteractionState

interface ItemListViewState : TrackLoadedViewState, LoadedLibraryState, LoadItemData {
	val itemValue: InteractionState<String>
	val items: InteractionState<List<IItem>>
	val loadedItem: IItem?
}
