package com.lasthopesoftware.bluewater.client.library.views.access

import com.lasthopesoftware.bluewater.client.library.items.Item
import com.namehillsoftware.handoff.promises.Promise

interface ProvideSelectedLibraryView {
	fun promiseSelectedOrDefaultView(): Promise<Item?>
}
