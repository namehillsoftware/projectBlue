package com.lasthopesoftware.bluewater.client.browsing.library.views.access

import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLibraryViews {
	fun promiseLibraryViews(): Promise<Collection<ViewItem>>
}
