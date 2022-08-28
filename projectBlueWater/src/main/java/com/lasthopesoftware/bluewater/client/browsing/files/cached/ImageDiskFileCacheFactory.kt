package com.lasthopesoftware.bluewater.client.browsing.files.cached

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.cached.access.CachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.AndroidDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.disk.IDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileAccessTimeUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.DiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.DiskFileCacheStreamSupplier
import com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.cache.ImageCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class ImageDiskFileCacheFactory private constructor(private val context: Context, private val libraryProvider: ILibraryProvider, private val diskCacheDirectoryProvider: IDiskCacheDirectoryProvider) : IProvideCaches {

	override fun promiseCache(libraryId: LibraryId): Promise<out CacheFiles?> =
		libraryProvider
			.getLibrary(libraryId)
			.then { library -> library?.let(::buildNewCache) }

	private fun buildNewCache(library: Library): CacheFiles {
		val imageCacheConfiguration = ImageCacheConfiguration(library)
		val cachedFilesProvider = CachedFilesProvider(context, imageCacheConfiguration)
		val diskFileAccessTimeUpdater = DiskFileAccessTimeUpdater(context)

		return DiskFileCache(
			context,
			diskCacheDirectoryProvider,
			imageCacheConfiguration,
			DiskFileCacheStreamSupplier(
				diskCacheDirectoryProvider,
				imageCacheConfiguration,
				DiskFileCachePersistence(
					context,
					diskCacheDirectoryProvider,
					imageCacheConfiguration,
					cachedFilesProvider,
					diskFileAccessTimeUpdater),
				cachedFilesProvider
			),
			cachedFilesProvider,
			diskFileAccessTimeUpdater)
	}

	companion object {

		fun getInstance(context: Context): ImageDiskFileCacheFactory {
			val libraryProvider = LibraryRepository(context)
			val diskCacheDirectoryProvider = AndroidDiskCacheDirectoryProvider(context)
			return ImageDiskFileCacheFactory(context, libraryProvider, diskCacheDirectoryProvider)
		}
	}
}
