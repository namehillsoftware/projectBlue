package com.lasthopesoftware.bluewater.client.library.views.access

import com.lasthopesoftware.bluewater.client.library.items.Item
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLibraryViews {
	fun promiseLibraryViews(): Promise<List<Item>>
}
