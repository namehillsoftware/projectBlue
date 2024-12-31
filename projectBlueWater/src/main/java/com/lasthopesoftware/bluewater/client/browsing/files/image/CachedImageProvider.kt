package com.lasthopesoftware.bluewater.client.browsing.files.image

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.images.bytes.GetImageBytes
import com.lasthopesoftware.bluewater.shared.images.bytes.cache.LookupImageCacheKey
import com.lasthopesoftware.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.policies.caching.LruPromiseCache
import com.namehillsoftware.handoff.promises.Promise

class CachedImageProvider(
	private val innerRawImages: GetImageBytes,
	private val cacheKeys: LookupImageCacheKey,
	private val rawCache: CachePromiseFunctions<String, ByteArray> = rawCompanionCache,
) : GetImageBytes {
	companion object {
		private const val MAX_MEMORY_CACHE_SIZE = 5

		private val rawCompanionCache by lazy { LruPromiseCache<String, ByteArray>(MAX_MEMORY_CACHE_SIZE) }
	}

	override fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray> =
		cacheKeys
			.promiseImageCacheKey(libraryId, serviceFile)
			.eventually { key -> rawCache.getOrAdd(key) { innerRawImages.promiseImageBytes(libraryId, serviceFile) } }

	override fun promiseImageBytes(libraryId: LibraryId, itemId: ItemId): Promise<ByteArray> =
		cacheKeys
			.promiseImageCacheKey(libraryId, itemId)
			.eventually { key -> rawCache.getOrAdd(key) { innerRawImages.promiseImageBytes(libraryId, itemId) } }
}
