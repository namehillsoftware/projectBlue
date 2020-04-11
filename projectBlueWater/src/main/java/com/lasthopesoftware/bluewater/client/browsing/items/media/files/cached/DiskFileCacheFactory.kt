package com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.access.CachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.disk.AndroidDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.disk.IDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.persistence.DiskFileAccessTimeUpdater
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.persistence.DiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.supplier.DiskFileCacheStreamSupplier
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ImageCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class DiskFileCacheFactory private constructor(private val context: Context, private val libraryProvider: ILibraryProvider, private val diskCacheDirectoryProvider: IDiskCacheDirectoryProvider) : IProvideCaches {

	private val syncObject = Object()
	private val sessionCaches = ConcurrentHashMap<LibraryId, ICache>()

	override fun promiseCache(libraryId: LibraryId): Promise<ICache> {
		val cache = sessionCaches[libraryId]
		if (cache != null) return Promise(cache)

		return synchronized(syncObject) {
			sessionCaches[libraryId]?.toPromise() ?: libraryProvider.getLibrary(libraryId)
				.then { library ->
					synchronized(syncObject) {
						when (val cache = sessionCaches[libraryId]) {
							null -> {
								val imageCacheConfiguration = ImageCacheConfiguration(library)
								val cachedFilesProvider = CachedFilesProvider(context, imageCacheConfiguration)
								val diskFileAccessTimeUpdater = DiskFileAccessTimeUpdater(context)

								sessionCaches[libraryId] = DiskFileCache(
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

								sessionCaches[libraryId]
							}
							else -> cache
						}
					}
				}
		}
	}

	companion object {

		private var instance: DiskFileCacheFactory? = null

		@JvmStatic
		@Synchronized
		fun getInstance(context: Context): DiskFileCacheFactory {
			val cachedInstance = instance
			if (cachedInstance != null) return cachedInstance

			val libraryProvider = LibraryRepository(context)
			val diskCacheDirectoryProvider = AndroidDiskCacheDirectoryProvider(context)
			val newInstance = DiskFileCacheFactory(context, libraryProvider, diskCacheDirectoryProvider)
			instance = newInstance
			return newInstance
		}
	}
}
