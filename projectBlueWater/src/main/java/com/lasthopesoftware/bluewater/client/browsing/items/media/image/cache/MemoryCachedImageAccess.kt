package com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache

import android.content.Context
import androidx.collection.LruCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.IProvideCaches
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.ImageDiskFileCacheFactory
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy

class MemoryCachedImageAccess private constructor(private val imageCacheKeys: LookupImageCacheKey, caches: IProvideCaches, connectionProvider: ProvideLibraryConnections)
	: DiskCacheImageAccess(imageCacheKeys, caches, connectionProvider) {

	companion object {
		private const val MAX_MEMORY_CACHE_SIZE = 10

		private lateinit var instance: MemoryCachedImageAccess

		@Synchronized
		@JvmStatic
		fun getInstance(context: Context): MemoryCachedImageAccess {
			if (::instance.isInitialized) return instance

			val libraryConnectionProvider = LibraryConnectionProvider.get(context)
			val filePropertiesCache = FilePropertyCache.getInstance()

			instance = MemoryCachedImageAccess(
				ImageCacheKeyLookup(CachedFilePropertiesProvider(
					libraryConnectionProvider,
					filePropertiesCache,
					FilePropertiesProvider(libraryConnectionProvider, filePropertiesCache))),
				ImageDiskFileCacheFactory.getInstance(context),
				libraryConnectionProvider)

			return instance
		}
	}

	private val syncObject = Object()

	private val imageMemoryCache = LruCache<String, ByteArray>(MAX_MEMORY_CACHE_SIZE)

	@Volatile
	private var currentCacheAccessPromise = Promise(ByteArray(0))

	override fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray> {
		return Promise(ImageOperator(libraryId, serviceFile))
	}

	inner class ImageOperator internal constructor(private val libraryId: LibraryId, private val serviceFile: ServiceFile) : MessengerOperator<ByteArray> {
		override fun send(messenger: Messenger<ByteArray>) {
			val promisedCacheKey = imageCacheKeys.promiseImageCacheKey(libraryId, serviceFile);

			val cancellationProxy = CancellationProxy()
			messenger.cancellationRequested(cancellationProxy)
			cancellationProxy.doCancel(promisedCacheKey)

			val promiseProxy = PromiseProxy(messenger)
			val promisedBytes = promisedCacheKey
				.eventually { uniqueKey ->
					val cachedBytes = imageMemoryCache[uniqueKey]
					if (cachedBytes != null && cachedBytes.isNotEmpty()) return@eventually cachedBytes.toPromise()

					synchronized(syncObject) {
						currentCacheAccessPromise = currentCacheAccessPromise
							.then({ imageMemoryCache[uniqueKey] }, { imageMemoryCache[uniqueKey] })
							.eventually { bytes ->
								if (bytes != null && bytes.isNotEmpty()) bytes.toPromise()
								else super@MemoryCachedImageAccess.promiseImageBytes(libraryId, serviceFile)
									.then {
										if (it.isNotEmpty()) imageMemoryCache.put(uniqueKey, it)
										it
									}
							}

						currentCacheAccessPromise
					}
				}
			promiseProxy.proxy(promisedBytes)
		}
	}
}
