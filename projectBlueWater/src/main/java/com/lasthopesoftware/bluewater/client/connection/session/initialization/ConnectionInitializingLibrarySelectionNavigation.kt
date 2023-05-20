package com.lasthopesoftware.bluewater.client.connection.session.initialization

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class ConnectionInitializingLibrarySelectionNavigation(
	private val inner: NavigateApplication,
	private val selectedLibraryViewModel: SelectedLibraryViewModel,
	private val connectionStatusViewModel: ConnectionStatusViewModel,
) : NavigateApplication by inner {
	override fun viewLibrary(libraryId: LibraryId): Promise<Unit> =
		selectConnection(libraryId) { inner.viewLibrary(libraryId) }

	override fun viewItem(libraryId: LibraryId, item: IItem): Promise<Unit> =
		selectConnection(libraryId) { inner.viewItem(libraryId, item) }

	override fun launchSearch(libraryId: LibraryId): Promise<Unit> =
		selectConnection(libraryId) { inner.launchSearch(libraryId) }

	override fun viewFileDetails(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int): Promise<Unit> =
		selectConnection(libraryId) { inner.viewFileDetails(libraryId, playlist, position) }

	override fun viewNowPlaying(libraryId: LibraryId): Promise<Unit> =
		selectConnection(libraryId) { inner.viewNowPlaying(libraryId) }

	private fun selectConnection(libraryId: LibraryId, onConnectionInitialized: () -> Promise<Unit>) =
		selectedLibraryViewModel
			.selectLibrary(libraryId)
			.eventually { connectionStatusViewModel.initializeConnection(libraryId) }
			.eventually {
				if (it) onConnectionInitialized()
				else Unit.toPromise()
			}
}
