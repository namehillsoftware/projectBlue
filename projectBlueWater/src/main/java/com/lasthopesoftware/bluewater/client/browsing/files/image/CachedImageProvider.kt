package com.lasthopesoftware.bluewater.client.browsing.files.image

import android.content.Context
import android.graphics.Bitmap
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.DiskFileCache
import com.lasthopesoftware.bluewater.client.browsing.files.cached.access.CachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.ImageCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.AndroidDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileAccessTimeUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.DiskFileCacheStreamSupplier
import com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.cache.DiskCacheImageAccess
import com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.cache.ImageCacheKeyLookup
import com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.cache.LookupImageCacheKey
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.policies.caching.LruPromiseCache
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class CachedImageProvider(
	private val inner: ProvideLibraryImages,
	private val cacheKeys: LookupImageCacheKey,
	private val cache: CachePromiseFunctions<String, Bitmap?>
) : ProvideLibraryImages {
	companion object {
		private const val MAX_MEMORY_CACHE_SIZE = 5

		private val cache by lazy { LruPromiseCache<String, Bitmap?>(MAX_MEMORY_CACHE_SIZE) }

		fun getInstance(context: Context): CachedImageProvider {
			val libraryConnectionProvider = ConnectionSessionManager.get(context)
			val filePropertiesCache = FilePropertyCache
			val imageCacheKeyLookup = ImageCacheKeyLookup(
				CachedFilePropertiesProvider(
					libraryConnectionProvider,
					filePropertiesCache,
					FilePropertiesProvider(
						libraryConnectionProvider,
						LibraryRevisionProvider(libraryConnectionProvider),
						filePropertiesCache
					)
				)
			)

			val imageCacheConfiguration = ImageCacheConfiguration
			val cachedFilesProvider = CachedFilesProvider(context, imageCacheConfiguration)
			val diskFileAccessTimeUpdater = DiskFileAccessTimeUpdater(context)
			val diskCacheDirectoryProvider = AndroidDiskCacheDirectoryProvider(context, imageCacheConfiguration)

			return CachedImageProvider(
				ScaledImageProvider(
					LibraryImageProvider(
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
										imageCacheConfiguration,
										cachedFilesProvider,
										diskFileAccessTimeUpdater
									),
									cachedFilesProvider
								),
								cachedFilesProvider,
								diskFileAccessTimeUpdater
							)
						)
					),
					context
				),
				imageCacheKeyLookup,
				cache
			)
		}
	}

	override fun promiseFileBitmap(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Bitmap?> =
		cacheKeys.promiseImageCacheKey(libraryId, serviceFile)
			.eventually { key -> cache.getOrAdd(key) { inner.promiseFileBitmap(libraryId, serviceFile) } }
			.keepPromise()
}
