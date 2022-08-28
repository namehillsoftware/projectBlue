package com.lasthopesoftware.bluewater.client.browsing.items.media.image.bytes.cache

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.IProvideCaches
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.bytes.GetRawImages
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.*

class DiskCacheImageAccess(private val sourceImages: GetRawImages, private val imageCacheKeys: LookupImageCacheKey, private val caches: IProvideCaches) : GetRawImages {

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(cls<DiskCacheImageAccess>()) }

		fun getBytesFromFiles(file: File, cancellationToken: CancellationToken): ByteArray {
			if (cancellationToken.isCancelled) return ByteArray(0)

			try {
				FileInputStream(file).use { fis ->
					ByteArrayOutputStream().use { buffer ->
						IOUtils.copy(fis, buffer)
						return buffer.toByteArray()
					}
				}
			} catch (e: FileNotFoundException) {
				logger.error("Could not find cached file.", e)
				return ByteArray(0)
			} catch (e: IOException) {
				logger.error("Error reading cached file.", e)
				return ByteArray(0)
			}
		}
	}

	override fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray> =
		CancellableProxyPromise(ImageOperator(libraryId, serviceFile))

	inner class ImageOperator internal constructor(private val libraryId: LibraryId, private val serviceFile: ServiceFile) : (CancellationProxy) -> Promise<ByteArray> {
		override fun invoke(cancellationProxy: CancellationProxy): Promise<ByteArray> =
			imageCacheKeys.promiseImageCacheKey(libraryId, serviceFile)
				.also(cancellationProxy::doCancel)
				.eventually { uniqueKey ->
					caches.promiseCache(libraryId)
						.eventually { cache ->
							cache
								?.promiseCachedFile(uniqueKey)
								?.also(cancellationProxy::doCancel)
								?.eventually { imageFile ->
									imageFile
										?.let { f -> QueuedPromise(ImageDiskCacheWriter(f), ThreadPools.io).also(cancellationProxy::doCancel) }
										.keepPromise()
								}
								?.eventually { bytes ->
									bytes?.toPromise() ?: sourceImages.promiseImageBytes(libraryId, serviceFile)
										.also { p ->
											cancellationProxy.doCancel(p)
											p.then { cache.put(uniqueKey, it) }.excuse { ioe -> logger.error("Error writing cached file!", ioe) }
										}
								}
								.keepPromise()
						}
				}
	}

	private class ImageDiskCacheWriter(private val imageCacheFile: File) : CancellableMessageWriter<ByteArray> {
		override fun prepareMessage(cancellationToken: CancellationToken): ByteArray = getBytesFromFiles(imageCacheFile, cancellationToken)
	}
}
