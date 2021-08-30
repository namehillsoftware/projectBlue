package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class SelectedBrowserLibraryProvider(private val selectedLibraryIdentifierProvider: ProvideSelectedLibraryId, private val libraryProvider: ILibraryProvider) : ISelectedBrowserLibraryProvider {
	override fun getBrowserLibrary(): Promise<Library?> =
		selectedLibraryIdentifierProvider.selectedLibraryId.eventually {
			it?.let(libraryProvider::getLibrary).keepPromise()
		}
}
