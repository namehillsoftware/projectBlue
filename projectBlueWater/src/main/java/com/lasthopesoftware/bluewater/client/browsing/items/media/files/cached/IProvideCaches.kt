package com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface IProvideCaches {
	fun promiseCache(libraryId: LibraryId): Promise<out ICache?>
}
