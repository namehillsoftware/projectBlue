package com.lasthopesoftware.bluewater.client.browsing.files.image

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.DiskFileCache
import com.lasthopesoftware.bluewater.client.browsing.files.cached.access.CachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.configuration.ImageCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.AndroidDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileAccessTimeUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.DiskFileCacheStreamSupplier
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.shared.android.ui.ScreenDimensions
import com.lasthopesoftware.bluewater.shared.images.bytes.GetRawImages
import com.lasthopesoftware.bluewater.shared.images.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.shared.images.bytes.cache.DiskCacheImageAccess
import com.lasthopesoftware.bluewater.shared.images.bytes.cache.ImageCacheKeyLookup
import com.lasthopesoftware.bluewater.shared.images.bytes.cache.LookupImageCacheKey
import com.lasthopesoftware.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.policies.caching.LruPromiseCache
import com.namehillsoftware.handoff.promises.Promise

class CachedImageProvider(
	private val innerRawImages: GetRawImages,
	private val cacheKeys: LookupImageCacheKey,
	private val rawCache: CachePromiseFunctions<String, ByteArray> = rawCompanionCache,
) : GetRawImages {
	companion object {
		private const val MAX_MEMORY_CACHE_SIZE = 5

		private val rawCompanionCache by lazy { LruPromiseCache<String, ByteArray>(MAX_MEMORY_CACHE_SIZE) }

		fun getInstance(context: Context): CachedImageProvider {
			val libraryConnectionProvider = ConnectionSessionManager.get(context)
			val filePropertiesCache = FilePropertyCache
			val imageCacheKeyLookup = ImageCacheKeyLookup(
				CachedFilePropertiesProvider(
					libraryConnectionProvider,
					filePropertiesCache,
					FilePropertiesProvider(
						GuaranteedLibraryConnectionProvider(libraryConnectionProvider),
						LibraryRevisionProvider(libraryConnectionProvider),
						filePropertiesCache
					)
				)
			)

			val imageCacheConfiguration = ImageCacheConfiguration
			val cachedFilesProvider = CachedFilesProvider(context, imageCacheConfiguration)
			val diskFileAccessTimeUpdater = DiskFileAccessTimeUpdater(context)
			val diskCacheDirectoryProvider = AndroidDiskCacheDirectoryProvider(context, imageCacheConfiguration)
			val rawImages = DiskCacheImageAccess(
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

			val scaledImageProvider = ScaledImageProvider(
				rawImages,
				ScreenDimensions(context),
			)

			return CachedImageProvider(
				scaledImageProvider,
				imageCacheKeyLookup,
				rawCompanionCache
			)
		}
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
