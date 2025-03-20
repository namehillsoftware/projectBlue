package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.files.list.LoadedLibraryState
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.menu.ActivityLaunching
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.parsedConnectionSettings
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ItemListViewModel(
	private val itemProvider: ProvideItems,
	messageBus: RegisterForApplicationMessages,
	private val libraryProvider: ILibraryProvider
) : ViewModel(), TrackLoadedViewState, LoadedLibraryState {

	private val activityLaunchingReceiver = messageBus.registerReceiver { event : ActivityLaunching ->
		mutableIsLoading.value = event != ActivityLaunching.HALTED // Only show the item list view again when launching error'ed for some reason
	}
	private val mutableItems = MutableStateFlow(emptyList<IItem>())
	private val mutableIsLoading = MutableInteractionState(true)
	private val mutableItemValue = MutableStateFlow("")

	private var loadedItem: Item? = null
	override var loadedLibraryId: LibraryId? = null
		private set

	val itemValue = mutableItemValue.asStateFlow()
	val items = mutableItems.asStateFlow()
	override val isLoading = mutableIsLoading.asInteractionState()

	override fun onCleared() {
		activityLaunchingReceiver.close()
	}

	fun loadItem(libraryId: LibraryId, item: Item? = null): Promise<Unit> {
		mutableIsLoading.value = loadedLibraryId != libraryId || loadedItem != item
		mutableItemValue.value = item?.value ?: ""
		loadedLibraryId = libraryId

		val promisedLibraryUpdate =
			if (item != null) Unit.toPromise()
			else libraryProvider
				.promiseLibrary(libraryId)
				.then { l ->
					mutableItemValue.value = l?.libraryName?.takeIf { it.isNotEmpty() } ?: l?.parsedConnectionSettings()?.accessCode ?: ""
				}

		val promisedItemUpdate = itemProvider
			.promiseItems(libraryId, item?.itemId)
			.then { items ->
				mutableItems.value = items
			}

		return Promise.whenAll(promisedItemUpdate, promisedLibraryUpdate)
			.then { _ ->
				loadedItem = item
			}
			.must { _ ->
				mutableIsLoading.value = false
			}
	}

	fun promiseRefresh(): Promise<Unit> = loadedLibraryId
		?.let { l ->
			val item = loadedItem
			loadedLibraryId = null
			loadedItem = null
			loadItem(l, item)
		}
		.keepPromise(Unit)
}
