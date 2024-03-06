package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class LibraryIdProviderViewModel : ProvideSelectedLibraryId, ViewModel() {
	private var cachedLibraryIdPromise = Promise.empty<LibraryId?>()

	override fun promiseSelectedLibraryId(): Promise<LibraryId?> = cachedLibraryIdPromise

	fun selectLibraryId(libraryId: LibraryId) {
		cachedLibraryIdPromise = libraryId.toPromise()
	}
}
