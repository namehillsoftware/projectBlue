package com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface LookupImageCacheKey {
	fun promiseImageCacheKey(libraryId: LibraryId, serviceFile: ServiceFile): Promise<String>
}
