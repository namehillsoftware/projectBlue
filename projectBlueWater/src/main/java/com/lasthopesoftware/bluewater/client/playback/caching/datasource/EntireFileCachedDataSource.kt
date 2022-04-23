package com.lasthopesoftware.bluewater.client.playback.caching.datasource

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.TransferListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.supplier.SupplyCacheStreams
import com.lasthopesoftware.bluewater.shared.drainQueue
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.bluewater.shared.promises.NoopResponse.Companion.noOpResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.lasthopesoftware.resources.uri.PathAndQuery.pathAndQuery
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.internal.closeQuietly
import okio.Buffer
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue

class EntireFileCachedDataSource(
	private val innerDataSource: HttpDataSource,
	private val cacheStreamSupplier: SupplyCacheStreams
) : DataSource {

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(EntireFileCachedDataSource::class.java) }
	}

	private var expectedFileSize = C.LENGTH_UNSET.toLong()
	private var downloadBytes = 0L
	private var promisedCacheWriter: Promise<CacheWriter>? = null

	override fun addTransferListener(transferListener: TransferListener) = innerDataSource.addTransferListener(transferListener)

	override fun open(dataSpec: DataSpec): Long {
		downloadBytes = 0

		val openedFileSize = innerDataSource.open(dataSpec)
		if (openedFileSize == C.LENGTH_UNSET.toLong()) return openedFileSize

		if (dataSpec.run { position != 0L || length != C.LENGTH_UNSET.toLong() }) return openedFileSize

		expectedFileSize = openedFileSize

		val key = dataSpec.uri.pathAndQuery()

		promisedCacheWriter?.then({ it.clear() }, noOpResponse())
		promisedCacheWriter = cacheStreamSupplier.promiseCachedFileOutputStream(key).then(::CacheWriter).apply {
			excuse {
				logger.warn("There was an error opening the cache output stream for key $key", it)
			}
		}

		return openedFileSize
	}

	override fun read(bytes: ByteArray, offset: Int, readLength: Int): Int {
		val result = innerDataSource.read(bytes, offset, readLength)

		val promisedWriter = promisedCacheWriter ?: return result

		if (result == C.RESULT_END_OF_INPUT && downloadBytes != expectedFileSize) {
			promisedWriter.then({ it.clear() }, noOpResponse())
			return result
		}

		if (result > 0) promisedWriter.then({ it.queueAndProcess(bytes, offset, result) }, noOpResponse())

		downloadBytes += result.coerceAtLeast(0).toLong()
		if (downloadBytes == expectedFileSize) {
			promisedWriter.then({ it.commit() }, noOpResponse())
			promisedCacheWriter = null
		}

		return result
	}

	override fun getUri(): Uri? = innerDataSource.uri

	override fun getResponseHeaders(): Map<String, List<String>> = innerDataSource.responseHeaders

	override fun close() {
		innerDataSource.close()
		promisedCacheWriter?.then({ it.clear() }, noOpResponse())
	}

	private class CacheWriter(private val cachedOutputStream: CacheOutputStream) {
		companion object {
			private const val goodBufferSize = 2 * 1024L * 1024L // 2MB
		}

		private val buffersToTransfer = ConcurrentLinkedQueue<Buffer>()
		private val activePromiseSync = Any()
		private val bufferSync = Any()
		private val rateLimiter = PromisingRateLimiter<Unit>(1)

		@Volatile
		private var isFaulted = false

		@Volatile
		private var activePromise = Unit.toPromise()

		@Volatile
		private var workingBuffer = Buffer()

		fun queueAndProcess(bytes: ByteArray, offset: Int, length: Int) {
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

		private fun processQueue() : Promise<Unit> =
			rateLimiter.limit {
				if (isFaulted) Unit.toPromise()
				else buffersToTransfer.drainQueue()
					.reduceOrNull { sink, source -> sink.also(source::readAll) }
					?.takeUnless { it.exhausted() }
					?.let(cachedOutputStream::promiseTransfer)
					?.apply {
						excuse {
							isFaulted = true
							logger.warn("An error occurred copying the buffer, closing the output stream", it)
							clear()
						}
					}
					?.unitResponse()
					.keepPromise(Unit)
			}.also { activePromise = it }

		fun commit() {
			synchronized(activePromiseSync) {
				activePromise = activePromise
					.eventually {
						synchronized(bufferSync) {
							buffersToTransfer.offer(workingBuffer)
						}

						processQueue()
					}
					.eventually { cachedOutputStream.flush() }
					.eventually { cachedOutputStream.commitToCache() }
					.must { cachedOutputStream.close() }
					.then { cachedOutputStream }
			}
		}

		fun clear() {
			synchronized(activePromiseSync) {
				activePromise.must {
					cachedOutputStream.closeQuietly()
					buffersToTransfer.drainQueue().forEach { it.clear() }
				}
			}
		}
	}

	class Factory(
		private val httpDataSourceFactory: HttpDataSource.Factory,
		private val cacheStreamSupplier: SupplyCacheStreams
	) : DataSource.Factory {
		override fun createDataSource(): DataSource = EntireFileCachedDataSource(
			httpDataSourceFactory.createDataSource(),
			cacheStreamSupplier
		)
	}
}
