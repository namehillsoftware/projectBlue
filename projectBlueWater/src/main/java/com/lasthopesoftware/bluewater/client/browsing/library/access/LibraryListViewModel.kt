package com.lasthopesoftware.bluewater.client.browsing.library.access

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.observables.MutableInteractionState
import com.namehillsoftware.handoff.promises.Promise

class LibraryListViewModel(
	private val libraryProvider: ProvideLibraries,
	private val libraryNameLookup: LookupLibraryName
) : ViewModel(), LibraryListState, TrackLoadedViewState {
	private val mutableLibraries = MutableInteractionState(emptyList<Pair<LibraryId, String>>())
	private val mutableIsLoading = MutableInteractionState(false)

	override val libraries = mutableLibraries.asInteractionState()
	override val isLoading = mutableIsLoading.asInteractionState()

	override fun loadLibraries(): Promise<Unit> = libraryProvider
		.promiseAllLibraries()
		.eventually {
			Promise.whenAll(
				it.map { l ->
					libraryNameLookup
						.promiseLibraryName(l.libraryId)
						.then { n -> Pair(l.libraryId, n ?: "") }
				}
			)
		}
		.then { it -> mutableLibraries.value = it.sortedBy { it.first.id } }
}
