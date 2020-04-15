package com.lasthopesoftware.bluewater.client.browsing.items.media.image

import android.graphics.Bitmap
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.ICache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.IProvideCaches
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedSessionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken
import java.io.File
import java.util.concurrent.CancellationException

class DiskCacheImageProvider(private val imageCacheKeys: LookupImageCacheKey, private val caches: IProvideCaches, private val selectedLibraryIdentifierProvider: ISelectedLibraryIdentifierProvider, connectionProvider: IConnectionProvider, cachedSessionFilePropertiesProvider: CachedSessionFilePropertiesProvider)
	: ImageProvider(selectedLibraryIdentifierProvider, connectionProvider, cachedSessionFilePropertiesProvider, caches) {

	inner class ImageOperator internal constructor(private val serviceFile: ServiceFile) : MessengerOperator<Bitmap> {
		override fun send(messenger: Messenger<Bitmap>) {
			val promisedCacheKey = imageCacheKeys.promiseImageCacheKey(serviceFile);

			val cancellationProxy = CancellationProxy()
			messenger.cancellationRequested(cancellationProxy)
			cancellationProxy.doCancel(promisedCacheKey)

			val promiseProxy = PromiseProxy(messenger)
			val promisedBitmap = promisedCacheKey
				.eventually { uniqueKey ->
					caches.promiseCache(selectedLibraryIdentifierProvider.selectedLibraryId)
								.eventually { cache ->
									cache.promiseCachedFile(uniqueKey)
										.eventually { imageFile -> QueuedPromise(ImageDiskCacheWriter(uniqueKey, imageFile), imageAccessExecutor.getObject())}
										.eventually { bitmap ->
											bitmap?.toPromise()
												?: super@DiskCacheImageProvider.promiseFileBitmap(serviceFile)
													.then {
														cache
															.put(uniqueKey, imageBytes)
															.excuse { ioe: Throwable? ->
																logger.error("Error writing cached file!", ioe)
																null
															}

														it
													}
										}
								}
				}
			promiseProxy.proxy(promisedBitmap)
		}
	}


	private class ImageDiskCacheWriter internal constructor(private val uniqueKey: String, private val imageCacheFile: File?) : MessageWriter<Bitmap?> {
		override fun prepareMessage(): Bitmap? {
			if (imageCacheFile != null) {
				val imageBytes = putBitmapIntoMemory(uniqueKey, imageCacheFile)
				if (imageBytes.isNotEmpty()) return getBitmapFromBytes(imageBytes)
			}
			return null
		}
	}

	private class RemoteImageAccessWriter private constructor(private val uniqueKey: String, private val imageDiskCache: ICache, private val imageBytes: ByteArray) : CancellableMessageWriter<Bitmap> {
		override fun prepareMessage(cancellationToken: CancellationToken): Bitmap {
			imageDiskCache
				.put(uniqueKey, imageBytes)
				.excuse { ioe: Throwable? ->
					logger.error("Error writing cached file!", ioe)
					null
				}

			if (cancellationToken.isCancelled) throw CancellationException(cancellationMessage)
			return getBitmapFromBytes(imageBytes)
		}

	}
}
