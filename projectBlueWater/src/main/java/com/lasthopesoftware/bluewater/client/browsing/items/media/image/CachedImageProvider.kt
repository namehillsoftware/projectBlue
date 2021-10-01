package com.lasthopesoftware.bluewater.client.browsing.items.media.image

import android.app.Activity
import android.graphics.Bitmap
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.bytes.cache.ImageCacheKeyLookup
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.bytes.cache.LookupImageCacheKey
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.bytes.cache.MemoryCachedImageAccess
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.StaticLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.bluewater.shared.policies.caching.LruPromiseCache
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class CachedImageProvider(
	private val inner: ProvideImages,
	private val selectedLibraryIdLookup: ProvideSelectedLibraryId,
	private val cacheKeys: LookupImageCacheKey,
	private val cache: CachePromiseFunctions<String, Bitmap?>
) : ProvideImages {
	companion object {
		private const val MAX_MEMORY_CACHE_SIZE = 10

		private val cache by lazy { LruPromiseCache<String, Bitmap?>(MAX_MEMORY_CACHE_SIZE) }

		fun getInstance(activity: Activity): CachedImageProvider {
			val selectedLibraryIdentifierProvider = SelectedBrowserLibraryIdentifierProvider(activity.getApplicationSettingsRepository())
			val libraryConnectionProvider = ConnectionSessionManager.get(activity)
			val filePropertiesCache = FilePropertyCache.getInstance()
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
					ImageProvider(
						StaticLibraryIdentifierProvider(selectedLibraryIdentifierProvider),
						MemoryCachedImageAccess.getInstance(activity)),
					activity),
				selectedLibraryIdentifierProvider,
				imageCacheKeyLookup,
				cache
			)
		}
	}

	override fun promiseFileBitmap(serviceFile: ServiceFile): Promise<Bitmap?> =
		selectedLibraryIdLookup
			.selectedLibraryId
			.eventually { libraryId ->
				libraryId
					?.let { l -> cacheKeys.promiseImageCacheKey(l, serviceFile) }
					?.eventually { key -> cache.getOrAdd(key) { inner.promiseFileBitmap(serviceFile) } }
					?.keepPromise()
			}
}
