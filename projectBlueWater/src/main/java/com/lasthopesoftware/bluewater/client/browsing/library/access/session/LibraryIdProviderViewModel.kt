package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class LibraryIdProviderViewModel : ProvideSelectedLibraryId, ViewModel() {
	private var cachedLibraryIdPromise = lazy { Promise(null as LibraryId?) }

	override fun promiseSelectedLibraryId(): Promise<LibraryId?> = cachedLibraryIdPromise.value

	fun updateLibraryId(libraryId: LibraryId) {
		cachedLibraryIdPromise = lazy { Promise(libraryId) }
	}
}
