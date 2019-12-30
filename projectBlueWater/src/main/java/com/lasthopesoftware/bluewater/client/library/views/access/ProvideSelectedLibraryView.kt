package com.lasthopesoftware.bluewater.client.library.views.access

import com.lasthopesoftware.bluewater.client.library.views.ViewItem
import com.namehillsoftware.handoff.promises.Promise

interface ProvideSelectedLibraryView {
	fun promiseSelectedOrDefaultView(): Promise<ViewItem?>
}
