package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.menu.ActivityLaunching
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ItemListViewModel(
	private val selectedLibraryId: ProvideSelectedLibraryId,
	private val itemProvider: ProvideItems,
	messageBus: RegisterForApplicationMessages
) : ViewModel(), (ActivityLaunching) -> Unit {

	private val receiver = messageBus.registerReceiver(this)
	private val mutableItems = MutableStateFlow(emptyList<Item>())
	private val mutableIsLoaded = MutableStateFlow(true)

	val items = mutableItems.asStateFlow()
	val isLoaded = mutableIsLoaded.asStateFlow()

	override fun invoke(event: ActivityLaunching) {
		mutableIsLoaded.value = event != ActivityLaunching.HALTED // Only show the item list view again when launching error'ed for some reason
	}

	override fun onCleared() {
		receiver.close()
	}

	fun loadItems(item: ItemId): Promise<Unit> {
		mutableIsLoaded.value = false
		return selectedLibraryId.selectedLibraryId
			.eventually {
				it?.let { itemProvider.promiseItems(it, item) }.keepPromise(emptyList())
			}
			.then {
				mutableItems.value = it
				mutableIsLoaded.value = true
			}
	}
}
