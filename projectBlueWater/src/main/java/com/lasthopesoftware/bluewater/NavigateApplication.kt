package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.promises.extensions.toPromise

interface NavigateApplication {

	fun viewLibrary(libraryId: LibraryId) = Unit.toPromise()

	fun viewApplicationSettings() = Unit.toPromise()

	fun viewHiddenSettings() = Unit.toPromise()

	fun viewNewServerSettings() = Unit.toPromise()

	fun viewServerSettings(libraryId: LibraryId) = Unit.toPromise()

	fun viewFileDetails(libraryId: LibraryId, item: IItem, positionedFile: PositionedFile) = Unit.toPromise()

	fun viewFileDetails(libraryId: LibraryId, searchQuery: String, positionedFile: PositionedFile) = Unit.toPromise()

	fun viewNowPlayingFileDetails(libraryId: LibraryId, positionedFile: PositionedFile) = Unit.toPromise()

	fun viewFileDetails(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int) = Unit.toPromise()

	fun launchSearch(libraryId: LibraryId) = Unit.toPromise()

	fun search(libraryId: LibraryId, filePropertyFilter: FileProperty) = Unit.toPromise()

	fun viewItem(libraryId: LibraryId, item: IItem) = Unit.toPromise()

	fun viewNowPlaying(libraryId: LibraryId) = Unit.toPromise()

	fun viewActiveDownloads(libraryId: LibraryId) = Unit.toPromise()

	/**
	 * Call this to navigate "up" in the navigation hierarchy.
	 * @return true if navigating up was handled, false if not handled.
	 */
	fun navigateUp() = true.toPromise()

	/**
	 * Call this to "back out" of the last user action. Note that this is more of an undo than [navigateUp].
	 * @return true if backing out was handled, false if not handled.
	 */
	fun backOut() = navigateUp()
}
