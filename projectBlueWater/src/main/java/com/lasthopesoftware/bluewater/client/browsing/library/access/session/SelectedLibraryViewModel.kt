package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.namehillsoftware.handoff.promises.Promise

class SelectedLibraryViewModel(
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val libraryBrowserSelection: SelectBrowserLibrary,
) : ViewModel(), TrackSelectedLibrary {

	private val mutableSelectedLibraryId = MutableInteractionState<LibraryId?>(null)

	override val selectedLibraryId = mutableSelectedLibraryId.asInteractionState()

	override fun loadSelectedLibraryId(): Promise<LibraryId?> =
		selectedLibraryIdProvider
			.promiseSelectedLibraryId()
			.then { it ->
				mutableSelectedLibraryId.value = it
				it
			}

	override fun selectLibrary(libraryId: LibraryId): Promise<LibraryId> =
		libraryBrowserSelection
			.selectBrowserLibrary(libraryId)
			.then { it ->
				mutableSelectedLibraryId.value = it.libraryId
				it.libraryId
			}
}
