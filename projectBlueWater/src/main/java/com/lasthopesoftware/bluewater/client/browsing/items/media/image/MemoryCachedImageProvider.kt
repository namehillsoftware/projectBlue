package com.lasthopesoftware.bluewater.client.browsing.items.media.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.IProvideCaches
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedSessionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.util.concurrent.CancellationException

class MemoryCachedImageProvider(cachedSessionFilePropertiesProvider: CachedSessionFilePropertiesProvider, private val imageCacheKeys: LookupImageCacheKey, selectedLibraryIdentifierProvider: ISelectedLibraryIdentifierProvider, connectionProvider: IConnectionProvider, caches: IProvideCaches)
	: ImageProvider(selectedLibraryIdentifierProvider, connectionProvider, cachedSessionFilePropertiesProvider, caches) {

	companion object {
		private const val MAX_MEMORY_CACHE_SIZE = 10
		private const val cancellationMessage = "The image task was cancelled"

		private val imageMemoryCache = LruCache<String, ByteArray>(MAX_MEMORY_CACHE_SIZE)

		private val logger = LoggerFactory.getLogger(MemoryCachedImageProvider::class.java)

		private fun getBitmapFromBytes(imageBytes: ByteArray): Bitmap {
			return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
		}

		private fun putBitmapIntoMemory(uniqueKey: String, imageBytes: ByteArray) {
			imageMemoryCache.put(uniqueKey, imageBytes)
		}

		private fun getBitmapBytesFromMemory(uniqueKey: String): ByteArray {
			return imageMemoryCache[uniqueKey] ?: return ByteArray(0)
		}
	}

	override fun promiseFileBitmap(serviceFile: ServiceFile): Promise<Bitmap> {
		return Promise(ImageOperator(serviceFile))
	}

	inner class ImageOperator internal constructor(private val serviceFile: ServiceFile) : MessengerOperator<Bitmap> {
		override fun send(messenger: Messenger<Bitmap>) {
			val promisedCacheKey = imageCacheKeys.promiseImageCacheKey(serviceFile);

			val cancellationProxy = CancellationProxy()
			messenger.cancellationRequested(cancellationProxy)
			cancellationProxy.doCancel(promisedCacheKey)

			val promiseProxy = PromiseProxy(messenger)
			val promisedBitmap = promisedCacheKey
//				.then { fileProperties ->
//					// First try storing by the album artist, which can cover the artist for the entire album (i.e. an album with various
//					// artists), and then by artist if that field is empty
//					var artist = fileProperties[KnownFileProperties.ALBUM_ARTIST]
//					if (artist == null || artist.isEmpty()) artist = fileProperties[KnownFileProperties.ARTIST]
//
//					var albumOrTrackName = fileProperties[KnownFileProperties.ALBUM]
//					if (albumOrTrackName == null) albumOrTrackName = fileProperties[KnownFileProperties.NAME]
//
//					"$artist:$albumOrTrackName"
//				}
				.eventually { uniqueKey ->
					val memoryTask = QueuedPromise(ImageMemoryWriter(uniqueKey), imageAccessExecutor.getObject());

					memoryTask.eventually { bitmap ->
						bitmap?.toPromise() ?: super@MemoryCachedImageProvider.promiseFileBitmap(serviceFile)
							.eventually { QueuedPromise(RemoteImageAccessWriter(uniqueKey, it), imageAccessExecutor.getObject())}
					}
				}
			promiseProxy.proxy(promisedBitmap)
		}
	}

	inner class ImageMemoryWriter internal constructor(private val uniqueKey: String) : CancellableMessageWriter<Bitmap?> {
		override fun prepareMessage(cancellationToken: CancellationToken): Bitmap? {
			if (cancellationToken.isCancelled) throw CancellationException(cancellationMessage)

			val imageBytes = getBitmapBytesFromMemory(uniqueKey)
			return if (imageBytes.isNotEmpty()) getBitmapFromBytes(imageBytes) else null
		}
	}

	private class RemoteImageAccessWriter internal constructor(private val uniqueKey: String, private val bitmap: Bitmap) : CancellableMessageWriter<Bitmap> {
		override fun prepareMessage(cancellationToken: CancellationToken): Bitmap {
			ByteArrayOutputStream().use {
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
				putBitmapIntoMemory(uniqueKey, it.toByteArray())
			}

			if (cancellationToken.isCancelled) throw CancellationException(cancellationMessage)

			val imageBytes = getBitmapBytesFromMemory(uniqueKey)
			return getBitmapFromBytes(imageBytes)
		}
	}
}
