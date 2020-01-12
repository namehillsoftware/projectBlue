package com.lasthopesoftware.bluewater.client.browsing.library.views.access

import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.namehillsoftware.handoff.promises.Promise

interface ProvideSelectedLibraryView {
	fun promiseSelectedOrDefaultView(): Promise<ViewItem?>
}
