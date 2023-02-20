package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

interface NavigateApplication {
	fun launchSettings() {}

	fun viewFileDetails(playlist: List<ServiceFile>, position: Int) {}

	fun launchSearch(libraryId: LibraryId) {}

	fun viewItem(libraryId: LibraryId, item: IItem) {}

	fun viewNowPlaying() {}

	fun viewActiveDownloads(libraryId: LibraryId) {}

	/**
	 * Call this to navigate "up" in the navigation hierarchy.
	 * @return true if navigating up was handled, false if not handled.
	 */
	fun navigateUp(): Boolean = true

	/**
	 * Call this to "back out" of the last user action. Note that this is more of an undo than [navigateUp].
	 * @return true if backing out was handled, false if not handled.
	 */
	fun backOut(): Boolean = navigateUp()
}
