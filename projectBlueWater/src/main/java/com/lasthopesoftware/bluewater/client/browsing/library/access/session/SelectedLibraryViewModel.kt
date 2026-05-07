package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.TrackLoadedViewState
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.observables.MutableInteractionState
import com.namehillsoftware.handoff.promises.Promise

class SelectedLibraryViewModel(
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val libraryBrowserSelection: SelectBrowserLibrary,
) : ViewModel(), TrackSelectedLibrary, TrackLoadedViewState {

	private val mutableSelectedLibraryId = MutableInteractionState<LibraryId?>(null)
	private val mutableIsLoading = MutableInteractionState(false)

	override val selectedLibraryId = mutableSelectedLibraryId.asInteractionState()

	override val isLoading = mutableIsLoading.asInteractionState()

	override fun promiseSelectedLibraryId(): Promise<LibraryId?> {
		mutableIsLoading.value = true
		return selectedLibraryIdProvider
			.promiseSelectedLibraryId()
			.then {
				mutableSelectedLibraryId.value = it
				it
			}
			.must { _ ->
				mutableIsLoading.value = false
			}
	}

	override fun selectBrowserLibrary(libraryId: LibraryId): Promise<Library> =
		libraryBrowserSelection
			.selectBrowserLibrary(libraryId)
			.then {
				mutableSelectedLibraryId.value = it.libraryId
				it
			}
}
