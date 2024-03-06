package com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.cache

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.CacheFiles
import com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.GetRawImages
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.ProvideProxyablePromise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

class DiskCacheImageAccess(
	private val sourceImages: GetRawImages,
	private val imageCacheKeys: LookupImageCacheKey,
	private val fileCache: CacheFiles,
) : GetRawImages {

	companion object {
		private val logger by lazyLogger<DiskCacheImageAccess>()
	}

	override fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray> =
		Promise.Proxy(ImageOperator(libraryId, serviceFile))

	inner class ImageOperator internal constructor(private val libraryId: LibraryId, private val serviceFile: ServiceFile) : ProvideProxyablePromise<ByteArray> {
		override fun provide(cancellationProxy: CancellationProxy): Promise<ByteArray> =
			imageCacheKeys.promiseImageCacheKey(libraryId, serviceFile)
				.also(cancellationProxy::doCancel)
				.eventually { uniqueKey ->
					fileCache
						.promiseCachedFile(libraryId, uniqueKey)
						.also(cancellationProxy::doCancel)
						.eventually { imageFile ->
							imageFile
								?.let { f -> QueuedPromise(ImageDiskCacheReader(f), ThreadPools.io).also(cancellationProxy::doCancel) }
								.keepPromise()
						}
						.eventually { bytes ->
							bytes
								?.takeIf { it.isNotEmpty() }
								?.toPromise()
								?: sourceImages
								.promiseImageBytes(libraryId, serviceFile)
								.also { p ->
									cancellationProxy.doCancel(p)
									p.then { b ->
										if (b.isNotEmpty())
											fileCache
												.put(libraryId, uniqueKey, b)
												.excuse { ioe -> logger.error("Error writing cached file!", ioe) }
									}
								}
						}
				}
	}

	private class ImageDiskCacheReader(private val imageCacheFile: File) : CancellableMessageWriter<ByteArray?> {
		override fun prepareMessage(cancellationToken: CancellationSignal): ByteArray? {
			if (cancellationToken.isCancelled) return null

			try {
				FileInputStream(imageCacheFile).use { fis ->
					ByteArrayOutputStream(fis.available()).use { buffer ->
						fis.copyTo(buffer)
						return buffer.toByteArray()
					}
				}
			} catch (e: FileNotFoundException) {
				logger.error("Could not find cached file.", e)
				return null
			} catch (e: IOException) {
				logger.error("Error reading cached file.", e)
				return null
			}
		}
	}
}
