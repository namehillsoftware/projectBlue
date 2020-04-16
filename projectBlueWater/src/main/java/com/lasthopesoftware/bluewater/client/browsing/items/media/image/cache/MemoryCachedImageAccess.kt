package com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache

import androidx.collection.LruCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.IProvideCaches
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy

class MemoryCachedImageAccess(private val imageCacheKeys: LookupImageCacheKey, caches: IProvideCaches, connectionProvider: ProvideLibraryConnections)
	: DiskCacheImageAccess(imageCacheKeys, caches, connectionProvider) {

	companion object {
		private const val MAX_MEMORY_CACHE_SIZE = 10

		private val imageMemoryCache = LruCache<String, ByteArray>(MAX_MEMORY_CACHE_SIZE)
	}

	override fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray> {
		return Promise(ImageOperator(libraryId, serviceFile))
	}

	inner class ImageOperator internal constructor(private val libraryId: LibraryId, private val serviceFile: ServiceFile) : MessengerOperator<ByteArray> {
		override fun send(messenger: Messenger<ByteArray>) {
			val promisedCacheKey = imageCacheKeys.promiseImageCacheKey(serviceFile);

			val cancellationProxy = CancellationProxy()
			messenger.cancellationRequested(cancellationProxy)
			cancellationProxy.doCancel(promisedCacheKey)

			val promiseProxy = PromiseProxy(messenger)
			val promisedBytes = promisedCacheKey
				.eventually { uniqueKey ->
					val cachedBytes = imageMemoryCache[uniqueKey]
					if (cachedBytes != null && cachedBytes.isNotEmpty()) Promise(cachedBytes)
					else super@MemoryCachedImageAccess.promiseImageBytes(libraryId, serviceFile)
						.then {
							if (it.isNotEmpty()) imageMemoryCache.put(uniqueKey, it)
							it
						}
				}
			promiseProxy.proxy(promisedBytes)
		}
	}
}
