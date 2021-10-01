package com.lasthopesoftware.bluewater.client.browsing.items.media.image.bytes.cache

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.ImageDiskFileCacheFactory
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.bytes.GetRawImages
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.shared.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy

class MemoryCachedImageAccess
(
	private val sourceImages: GetRawImages,
	private val imageCacheKeys: LookupImageCacheKey,
	private val cache: CachePromiseFunctions<String, ByteArray>
): GetRawImages {

	companion object {
		private const val MAX_MEMORY_CACHE_SIZE = 5

		private val cache by lazy { LruPromiseCache<String, ByteArray>(MAX_MEMORY_CACHE_SIZE) }

		fun getInstance(context: Context): MemoryCachedImageAccess {
			val libraryConnectionProvider = ConnectionSessionManager.get(context)
			val filePropertiesCache = FilePropertyCache.getInstance()
			val imageCacheKeyLookup = ImageCacheKeyLookup(CachedFilePropertiesProvider(
				libraryConnectionProvider,
				filePropertiesCache,
				FilePropertiesProvider(
					libraryConnectionProvider,
					LibraryRevisionProvider(libraryConnectionProvider),
					filePropertiesCache)))

			return MemoryCachedImageAccess(
				DiskCacheImageAccess(
					RemoteImageAccess(libraryConnectionProvider),
					imageCacheKeyLookup,
					ImageDiskFileCacheFactory.getInstance(context)),
				imageCacheKeyLookup,
				cache
            )
		}
	}

	override fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray> =
		Promise(ImageOperator(libraryId, serviceFile))

	inner class ImageOperator internal constructor(private val libraryId: LibraryId, private val serviceFile: ServiceFile) : MessengerOperator<ByteArray> {
		override fun send(messenger: Messenger<ByteArray>) {
			val promisedCacheKey = imageCacheKeys.promiseImageCacheKey(libraryId, serviceFile)

			val promiseProxy = PromiseProxy(messenger)
			val promisedBytes = promisedCacheKey
				.eventually { uniqueKey ->
					cache.getOrAdd(uniqueKey) { sourceImages.promiseImageBytes(libraryId, serviceFile) }
				}
			promiseProxy.proxy(promisedBytes)
		}
	}
}
