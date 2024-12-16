package com.lasthopesoftware.bluewater.shared.images.bytes.cache

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface LookupImageCacheKey {
	fun promiseImageCacheKey(libraryId: LibraryId, serviceFile: ServiceFile): Promise<String>
	fun promiseImageCacheKey(libraryId: LibraryId, itemId: ItemId): Promise<String>
}
