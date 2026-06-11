package com.lasthopesoftware.bluewater.client.connection.session.initialization

import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.TrackSelectedLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.ForwardedResponse.Companion.forward
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse

class LibrarySelectionNavigation(
	private val inner: NavigateApplication,
	private val selectedLibraryViewModel: TrackSelectedLibrary,
	private val connectionStatus: TrackConnectionStatus,
) : NavigateApplication by inner {
	companion object {
		private val logger by lazyLogger<LibrarySelectionNavigation>()
	}

	override fun viewActiveLibrary(): Promise<Unit> =
		promiseSelectedLibraryId()
			.eventually {
				it?.let(::viewLibrary) ?: viewApplicationSettings()
			}

	override fun searchActiveLibrary(searchQuery: String): Promise<Unit> =
		promiseSelectedLibraryId()
			.eventually { l ->
				l?.let { search(it, searchQuery) } ?: viewApplicationSettings()
			}

	override fun viewActiveDownloads(): Promise<Unit> =
		promiseSelectedLibraryId()
			.eventually {
				it?.let(inner::viewActiveDownloads) ?: backOut().unitResponse()
			}

	override fun viewLibrary(libraryId: LibraryId): Promise<Unit> =
		selectLibrary(libraryId) { l ->
			connectionStatus
				.initializeConnection(l.libraryId)
				.cancelBackEventually {
					if (it) inner.viewLibrary(l.libraryId)
					else inner.viewApplicationSettings()
				}
		}

	override fun viewItem(libraryId: LibraryId, item: IItem): Promise<Unit> =
		selectLibrary(libraryId) { inner.viewItem(it.libraryId, item) }

	override fun launchSearch(libraryId: LibraryId): Promise<Unit> =
		selectLibrary(libraryId) { inner.launchSearch(it.libraryId) }

	override fun search(libraryId: LibraryId, filePropertyFilter: FileProperty): Promise<Unit> =
		selectLibrary(libraryId) { inner.search(it.libraryId, filePropertyFilter) }

	override fun search(libraryId: LibraryId, searchQuery: String): Promise<Unit> =
		selectLibrary(libraryId) { inner.search(it.libraryId, searchQuery) }

	override fun viewFileDetails(libraryId: LibraryId, searchQuery: String, positionedFile: PositionedFile): Promise<Unit> =
		selectLibrary(libraryId) { inner.viewFileDetails(it.libraryId, searchQuery, positionedFile) }

	override fun viewFileDetails(libraryId: LibraryId, item: IItem?, positionedFile: PositionedFile): Promise<Unit> =
		selectLibrary(libraryId) { inner.viewFileDetails(it.libraryId, item, positionedFile) }

	override fun viewNowPlaying(libraryId: LibraryId): Promise<Unit> =
		selectLibrary(libraryId) { inner.viewNowPlaying(it.libraryId) }

	override fun viewActiveDownloads(libraryId: LibraryId): Promise<Unit> =
		selectLibrary(libraryId) { inner.viewActiveDownloads(it.libraryId) }

	private fun selectLibrary(libraryId: LibraryId, onLibrarySelected: PromisedResponse<Library, Unit>) =
		selectedLibraryViewModel
			.selectBrowserLibrary(libraryId)
			.eventually(onLibrarySelected)

	private fun promiseSelectedLibraryId(): Promise<LibraryId> =
		selectedLibraryViewModel
			.promiseSelectedLibraryId()
			.then(forward()) { e ->
				logger.error("An error occurred initializing the library", e)
				null
			}
}
