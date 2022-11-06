package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

interface NavigateApplication {
	fun viewFileDetails(playlist: List<ServiceFile>, position: Int) {}

	fun launchSearch() {}

	fun viewItem(item: IItem) {}

	fun viewNowPlaying() {}

	fun viewConnectionRestoration(): Promise<Unit> = Unit.toPromise()

	fun backOut(): Boolean = true
}
