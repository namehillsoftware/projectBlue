package com.lasthopesoftware.bluewater.client.browsing.files.image

import android.content.Context
import android.graphics.Bitmap
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.ImageDiskFileCacheFactory
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
import com.lasthopesoftware.bluewater.shared.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
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
						filePropertiesCache)
				)
			)

			return CachedImageProvider(
				ScaledImageProvider(
					LibraryImageProvider(
						DiskCacheImageAccess(
							RemoteImageAccess(libraryConnectionProvider),
							imageCacheKeyLookup,
							ImageDiskFileCacheFactory.getInstance(context))
					),
					context),
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
