package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.files.list.LoadedLibraryState
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.LoadItemData
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.menu.ActivityLaunching
import com.lasthopesoftware.bluewater.client.browsing.library.access.LookupLibraryName
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class ItemListViewModel(
	private val itemProvider: ProvideItems,
	messageBus: RegisterForApplicationMessages,
	private val libraryNameLookup: LookupLibraryName,
) : ViewModel(), TrackLoadedViewState, LoadedLibraryState, LoadItemData {

	private val activityLaunchingReceiver = messageBus.registerReceiver { event : ActivityLaunching ->
		mutableIsLoading.value = event != ActivityLaunching.HALTED // Only show the item list view again when launching error'ed for some reason
	}
	private val mutableItems = MutableInteractionState(emptyList<IItem>())
	private val mutableIsLoading = MutableInteractionState(true)
	private val mutableItemValue = MutableInteractionState("")

	var loadedItem: IItem? = null
		private set
	override var loadedLibraryId: LibraryId? = null
		private set

	val itemValue = mutableItemValue.asInteractionState()
	val items = mutableItems.asInteractionState()
	override val isLoading = mutableIsLoading.asInteractionState()

	override fun onCleared() {
		activityLaunchingReceiver.close()
	}

	override fun loadItem(libraryId: LibraryId, item: IItem?): Promise<Unit> {
		mutableIsLoading.value = loadedLibraryId != libraryId || loadedItem != item
		mutableItemValue.value = item?.value ?: ""
		loadedLibraryId = libraryId

		return Promise.Proxy { cs ->
			val promisedLibraryUpdate =
				if (item != null) Unit.toPromise()
				else libraryNameLookup
					.promiseLibraryName(libraryId)
					.then { n -> mutableItemValue.value = n ?: "" }

			val promisedItemUpdate = itemProvider
				.promiseItems(libraryId, item?.itemId)
				.also(cs::doCancel)
				.then { items ->
					mutableItems.value = items
				}

			Promise
				.whenAll(promisedItemUpdate, promisedLibraryUpdate)
				.then { _ -> loadedItem = item }
				.must { _ -> mutableIsLoading.value = false }
		}
	}

	override fun promiseRefresh(): Promise<Unit> = loadedLibraryId
		?.let { l ->
			val item = loadedItem
			loadedLibraryId = null
			loadedItem = null
			loadItem(l, item)
		}
		.keepPromise(Unit)
}
