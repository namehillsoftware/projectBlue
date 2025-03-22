package com.lasthopesoftware.bluewater.client.playback.caching.datasource

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.SupplyCacheStreams
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.drainQueue
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.promises.ForwardedResponse.Companion.forward
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.uri.PathAndQuery.pathAndQuery
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.internal.closeQuietly
import okio.Buffer
import java.util.concurrent.ConcurrentLinkedQueue

@UnstableApi class EntireFileCachedDataSource(
	private val libraryId: LibraryId,
	private val innerDataSource: DataSource,
	private val cacheStreamSupplier: SupplyCacheStreams
) : DataSource {

	companion object {
		private val logger by lazyLogger<EntireFileCachedDataSource>()
	}

	private var expectedFileSize = C.LENGTH_UNSET.toLong()
	private var downloadBytes = 0L
	private var cacheWriter: CacheWriter? = null

	override fun addTransferListener(transferListener: TransferListener) = innerDataSource.addTransferListener(transferListener)

	override fun open(dataSpec: DataSpec): Long {
		downloadBytes = 0

		val openedFileSize = innerDataSource.open(dataSpec)
		if (openedFileSize == C.LENGTH_UNSET.toLong()) return openedFileSize

		if (dataSpec.run { position != 0L || length != C.LENGTH_UNSET.toLong() }) return openedFileSize

		expectedFileSize = openedFileSize

		val key = dataSpec.uri.pathAndQuery()

		cacheWriter?.clear()
		cacheWriter = CacheWriter(
			cacheStreamSupplier.promiseCachedFileOutputStream(libraryId, key)
				.then(forward()) {
					logger.warn("There was an error opening the cache output stream for key $key", it)
					null
				})

		return openedFileSize
	}

	override fun read(bytes: ByteArray, offset: Int, readLength: Int): Int {
		val result = innerDataSource.read(bytes, offset, readLength)

		val writer = cacheWriter ?: return result

		if (result == C.RESULT_END_OF_INPUT && downloadBytes != expectedFileSize) {
			writer.clear()
			return result
		}

		if (result > 0) writer.queueAndProcess(bytes, offset, result)

		downloadBytes += result.coerceAtLeast(0).toLong()
		if (downloadBytes == expectedFileSize) {
			writer.commit()
			cacheWriter = null
		}

		return result
	}

	override fun getUri(): Uri? = innerDataSource.uri

	override fun getResponseHeaders(): Map<String, List<String>> = innerDataSource.responseHeaders

	override fun close() {
		innerDataSource.close()
		cacheWriter?.clear()
	}

	private class CacheWriter(private val promisedOutputStream: Promise<CacheOutputStream?>) {
		companion object {
			private const val goodBufferSize = 2 * 1024L * 1024L // 2MB
		}

		private val buffersToTransfer = ConcurrentLinkedQueue<Buffer>()
		private val activePromiseSync = Any()
		private val bufferSync = Any()
		private val rateLimiter = PromisingRateLimiter<CacheOutputStream?>(1)

		@Volatile
		private var isFaulted = false

		@Volatile
		private var activePromise: Promise<CacheOutputStream?> = promisedOutputStream

		@Volatile
		private var workingBuffer = Buffer()

		fun queueAndProcess(bytes: ByteArray, offset: Int, length: Int) {
			if (isFaulted) return

			synchronized(bufferSync) {
				workingBuffer.write(bytes, offset, length)
				if (workingBuffer.size < goodBufferSize) return

				buffersToTransfer.offer(workingBuffer)

				workingBuffer = Buffer()
			}

			synchronized(activePromiseSync) {
				activePromise = processQueue()
			}
		}

		private fun processQueue() : Promise<CacheOutputStream?> = rateLimiter.limit {
			promisedOutputStream.eventually { outputStream ->
				if (outputStream == null) isFaulted = true

				if (outputStream == null || isFaulted) Promise.empty<CacheOutputStream?>()
				else buffersToTransfer.drainQueue()
					.reduceOrNull { sink, source -> sink.also(source::readAll) }
					?.takeUnless { it.exhausted() }
					?.let(outputStream::promiseTransfer)
					?.apply {
						excuse { it ->
							isFaulted = true
							logger.warn("An error occurred copying the buffer, closing the output stream", it)
							clear()
						}
					}
					.keepPromise(outputStream)
			}
		}

		fun commit() {
			if (isFaulted) {
				clear()
				return
			}

			synchronized(activePromiseSync) {
				activePromise = activePromise
					.eventually {
						synchronized(bufferSync) {
							buffersToTransfer.offer(workingBuffer)
						}

						processQueue()
					}
					.eventually { it?.flush().keepPromise() }
					.eventually { os -> os?.commitToCache()?.must { _ -> os.close() }?.then { _ -> os }.keepPromise() }
			}
		}

		fun clear() {
			synchronized(activePromiseSync) {
				activePromise.must { _ ->
					promisedOutputStream.then {	os ->
						os?.closeQuietly()
						buffersToTransfer.drainQueue().forEach { it.clear() }
					}
				}
			}
		}
	}

	@UnstableApi class Factory(
		private val libraryId: LibraryId,
		private val httpDataSourceFactory: DataSource.Factory,
		private val cacheStreamSupplier: SupplyCacheStreams
	) : DataSource.Factory {

		override fun createDataSource(): DataSource = EntireFileCachedDataSource(
			libraryId,
			httpDataSourceFactory.createDataSource(),
			cacheStreamSupplier
		)
	}
}
