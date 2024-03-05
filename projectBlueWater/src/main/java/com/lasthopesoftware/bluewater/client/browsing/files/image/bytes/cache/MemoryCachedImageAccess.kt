package com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.cache

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.DiskFileCache
import com.lasthopesoftware.bluewater.client.browsing.files.cached.access.CachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.AndroidDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileAccessTimeUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.DiskFileCacheStreamSupplier
import com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.GetRawImages
import com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.shared.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.ProxyPromise

class MemoryCachedImageAccess
(
	private val sourceImages: GetRawImages,
	private val imageCacheKeys: LookupImageCacheKey,
	private val cache: CachePromiseFunctions<String, ByteArray>
): GetRawImages {

	companion object {
		private const val MAX_MEMORY_CACHE_SIZE = 10

		private val cache by lazy { LruPromiseCache<String, ByteArray>(MAX_MEMORY_CACHE_SIZE) }

		fun getInstance(context: Context): MemoryCachedImageAccess {
			val libraryConnectionProvider = ConnectionSessionManager.get(context)
			val filePropertiesCache = FilePropertyCache
			val imageCacheKeyLookup = ImageCacheKeyLookup(CachedFilePropertiesProvider(
				libraryConnectionProvider,
				filePropertiesCache,
				FilePropertiesProvider(
					libraryConnectionProvider,
					LibraryRevisionProvider(libraryConnectionProvider),
					filePropertiesCache)))

			val imageCacheConfiguration = ImageCacheConfiguration
			val cachedFilesProvider = CachedFilesProvider(context, imageCacheConfiguration)
			val diskFileAccessTimeUpdater = DiskFileAccessTimeUpdater(context)
			val diskCacheDirectoryProvider = AndroidDiskCacheDirectoryProvider(context, imageCacheConfiguration)

			return MemoryCachedImageAccess(
				DiskCacheImageAccess(
					RemoteImageAccess(libraryConnectionProvider),
					imageCacheKeyLookup,
					DiskFileCache(
						context,
						diskCacheDirectoryProvider,
						imageCacheConfiguration,
						DiskFileCacheStreamSupplier(
							diskCacheDirectoryProvider,
							DiskFileCachePersistence(
								context,
								diskCacheDirectoryProvider,
								imageCacheConfiguration,
								cachedFilesProvider,
								diskFileAccessTimeUpdater
							),
							cachedFilesProvider
						),
						cachedFilesProvider,
						diskFileAccessTimeUpdater
					)
				),
				imageCacheKeyLookup,
				cache
            )
		}
	}

	override fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray> =
		PromisedImage(libraryId, serviceFile)

	inner class PromisedImage internal constructor(private val libraryId: LibraryId, private val serviceFile: ServiceFile) : ProxyPromise<ByteArray>() {

		init {
			val promisedCacheKey = imageCacheKeys.promiseImageCacheKey(libraryId, serviceFile)
			val promisedBytes = promisedCacheKey
				.eventually { uniqueKey ->
					cache.getOrAdd(uniqueKey) { sourceImages.promiseImageBytes(libraryId, serviceFile) }
				}
			proxy(promisedBytes)
		}
	}
}
