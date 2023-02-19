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

	fun navigateUp(): Boolean = true

	fun backOut(): Boolean = navigateUp()
}
