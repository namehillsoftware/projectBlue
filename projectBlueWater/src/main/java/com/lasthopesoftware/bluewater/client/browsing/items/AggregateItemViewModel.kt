package com.lasthopesoftware.bluewater.client.browsing.items

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.observables.InteractionState
import com.lasthopesoftware.bluewater.shared.observables.LiftedInteractionState
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
import com.lasthopesoftware.promises.extensions.UnitResponse
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.closables.AutoCloseableManager
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.Observable

class AggregateItemViewModel(
	vararg itemData: LoadItemData
) : ViewModel(), LoadItemData {
	private val autoCloseableManager = AutoCloseableManager()
	private val itemDataLoaders = arrayOf(*itemData)

	override val isLoading: InteractionState<Boolean> by lazy {
		autoCloseableManager.manage(
			LiftedInteractionState(
				Observable.combineLatest(itemDataLoaders.map { it.isLoading.mapNotNull() }) { sources ->
					sources.any { it as Boolean }
				},
				false
			)
		)
	}

	override fun loadItem(libraryId: LibraryId, item: IItem?): Promise<Unit> {
		val promisedLoads = itemDataLoaders.map { it.loadItem(libraryId, item) }

		return Promise.whenAll(promisedLoads)
			.then(
				UnitResponse.respond(),
				{
					for (promisedLoad in promisedLoads)
						promisedLoad.cancel()
					throw it
				}
			)
	}

	override fun promiseRefresh(): Promise<Unit> =
		Promise.whenAll(itemDataLoaders.map { it.promiseRefresh() }).unitResponse()

	override fun onCleared() {
		autoCloseableManager.close()
	}
}
