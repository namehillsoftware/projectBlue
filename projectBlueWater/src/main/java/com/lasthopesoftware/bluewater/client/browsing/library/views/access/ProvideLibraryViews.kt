package com.lasthopesoftware.bluewater.client.browsing.library.views.access

import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.namehillsoftware.handoff.promises.Promise

fun interface ProvideLibraryViews {
	fun promiseLibraryViews(): Promise<Collection<ViewItem>>
}
