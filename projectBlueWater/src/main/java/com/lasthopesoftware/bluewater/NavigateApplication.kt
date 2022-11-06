package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.IItem

interface NavigateApplication {
	fun viewFileDetails(playlist: List<ServiceFile>, position: Int) {}

	fun launchSearch() {}

	fun viewItem(item: IItem) {}

	fun viewNowPlaying() {}
}
