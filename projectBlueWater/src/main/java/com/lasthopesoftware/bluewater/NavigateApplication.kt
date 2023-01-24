package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

interface NavigateApplication {
	fun viewFileDetails(playlist: List<ServiceFile>, position: Int) {}

	fun launchSearch() {}

	fun viewItem(libraryId: LibraryId, item: IItem) {}

	fun viewNowPlaying() {}

	fun backOut(): Boolean = true
}
