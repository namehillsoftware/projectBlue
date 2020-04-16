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
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy
import java.util.concurrent.locks.ReentrantReadWriteLock

class MemoryCachedImageAccess private constructor(private val imageCacheKeys: LookupImageCacheKey, caches: IProvideCaches, connectionProvider: ProvideLibraryConnections)
	: DiskCacheImageAccess(imageCacheKeys, caches, connectionProvider) {

	companion object {
		private const val MAX_MEMORY_CACHE_SIZE = 10

		private val imageMemoryCache = LruCache<String, ByteArray>(MAX_MEMORY_CACHE_SIZE)

		private var instance: MemoryCachedImageAccess? = null

		@Synchronized
		@JvmStatic
		fun getInstance(context: Context): MemoryCachedImageAccess {
			val cachedInstance = instance
			if (cachedInstance != null) return cachedInstance

			val libraryConnectionProvider = LibraryConnectionProvider.get(context)
			val filePropertiesCache = FilePropertyCache.getInstance()

			val newInstance = MemoryCachedImageAccess(
				ImageCacheKeyLookup(CachedFilePropertiesProvider(
					libraryConnectionProvider,
					filePropertiesCache,
					FilePropertiesProvider(libraryConnectionProvider, filePropertiesCache))),
				ImageDiskFileCacheFactory.getInstance(context),
				libraryConnectionProvider)

			instance = newInstance
			return newInstance
		}
	}

	private val readWriteLock = ReentrantReadWriteLock()

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
					var cachedBytes = imageMemoryCache[uniqueKey]
					if (cachedBytes != null && cachedBytes.isNotEmpty()) return@eventually Promise(cachedBytes)

					val lock = readWriteLock.writeLock()
					cachedBytes = imageMemoryCache[uniqueKey]
					if (cachedBytes != null && cachedBytes.isNotEmpty()) {
						lock.unlock()
						return@eventually Promise(cachedBytes)
					}

					super@MemoryCachedImageAccess.promiseImageBytes(libraryId, serviceFile)
						.then {
							if (it.isNotEmpty()) imageMemoryCache.put(uniqueKey, it)
							it
						}
						.must { lock.unlock() }
				}
			promiseProxy.proxy(promisedBytes)
		}
	}
}
