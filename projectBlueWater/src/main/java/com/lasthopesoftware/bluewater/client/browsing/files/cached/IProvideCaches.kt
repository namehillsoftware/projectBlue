package com.lasthopesoftware.bluewater.client.browsing.files.cached

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface IProvideCaches {
	fun promiseCache(libraryId: LibraryId): Promise<out CacheFiles?>
}
