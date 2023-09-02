package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SelectedLibraryViewModel(
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val libraryBrowserSelection: SelectBrowserLibrary,
) : ViewModel() {

	private val mutableSelectedLibraryId = MutableStateFlow<LibraryId?>(null)

	val selectedLibraryId = mutableSelectedLibraryId.asStateFlow()

	fun loadSelectedLibraryId(): Promise<LibraryId?> =
		selectedLibraryIdProvider
			.promiseSelectedLibraryId()
			.then {
				mutableSelectedLibraryId.value = it
				it
			}

	fun selectLibrary(libraryId: LibraryId): Promise<LibraryId> =
		libraryBrowserSelection
			.selectBrowserLibrary(libraryId)
			.then {
				mutableSelectedLibraryId.value = it.libraryId
				it.libraryId
			}
}
