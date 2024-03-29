package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface HoldSelectedLibraryId {
	fun getOrCache(factory: () -> Promise<LibraryId?>): Promise<LibraryId?>
}
