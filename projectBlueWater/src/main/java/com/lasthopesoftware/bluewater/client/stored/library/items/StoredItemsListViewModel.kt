package com.lasthopesoftware.bluewater.client.stored.library.items

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class StoredItemsListViewModel(
	private val storedItemAccess: AccessStoredItems
) : ViewModel(), TrackLoadedViewState {
	private val mutableIsLoading = MutableInteractionState(false)

	override val isLoading = mutableIsLoading.asInteractionState()

	fun loadItems(): Promise<Unit> {
		mutableIsLoading.value = true
		return Unit.toPromise()
			.must { _ -> mutableIsLoading.value = false }
	}
}
