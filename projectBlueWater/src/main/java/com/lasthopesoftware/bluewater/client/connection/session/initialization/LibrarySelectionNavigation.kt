package com.lasthopesoftware.bluewater.client.connection.session.initialization

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.TrackSelectedLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse

class LibrarySelectionNavigation(
	private val inner: NavigateApplication,
	private val selectedLibraryViewModel: TrackSelectedLibrary,
	private val connectionStatus: TrackConnectionStatus,
) : NavigateApplication by inner {
	override fun viewLibrary(libraryId: LibraryId): Promise<Unit> =
		selectConnection(libraryId) { id ->
			Promise.Proxy { cp ->
				connectionStatus
					.initializeConnection(id)
					.also(cp::doCancel)
					.eventually {
						if (it) inner.viewLibrary(id)
						else inner.viewApplicationSettings()
					}
			}
		}

	override fun viewItem(libraryId: LibraryId, item: IItem): Promise<Unit> =
		selectConnection(libraryId) { inner.viewItem(libraryId, item) }

	override fun launchSearch(libraryId: LibraryId): Promise<Unit> =
		selectConnection(libraryId) { inner.launchSearch(libraryId) }

	override fun search(libraryId: LibraryId, filePropertyFilter: FileProperty): Promise<Unit> =
		selectConnection(libraryId) { inner.search(libraryId, filePropertyFilter) }

	override fun viewFileDetails(libraryId: LibraryId, files: List<ServiceFile>, position: Int): Promise<Unit> =
		selectConnection(libraryId) { inner.viewFileDetails(libraryId, files, position) }

	override fun viewNowPlaying(libraryId: LibraryId): Promise<Unit> =
		selectConnection(libraryId) { inner.viewNowPlaying(libraryId) }

	private fun selectConnection(libraryId: LibraryId, onLibrarySelected: PromisedResponse<LibraryId, Unit>) =
		selectedLibraryViewModel
			.selectLibrary(libraryId)
			.eventually(onLibrarySelected)
}
