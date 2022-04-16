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
import com.lasthopesoftware.bluewater.shared.promises.NoopResponse.Companion.ignore
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.toFuture
import com.lasthopesoftware.resources.uri.PathAndQuery
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.internal.closeQuietly
import okio.Buffer
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue

class FullyDownloadedCachedFilesDataSource(
	private val innerDataSource: HttpDataSource,
	private val cacheStreamSupplier: SupplyCacheStreams
) : DataSource {
	private var cacheWriter: CacheWriter? = null

	override fun addTransferListener(transferListener: TransferListener) = innerDataSource.addTransferListener(transferListener)

	override fun open(dataSpec: DataSpec): Long {
		cacheWriter?.commit()?.also { cacheWriter = null }

		val key = PathAndQuery.forUri(dataSpec.uri)

		if (with(dataSpec) { position == 0L && length == C.LENGTH_UNSET.toLong() }) {
			cacheWriter = cacheStreamSupplier.promiseCachedFileOutputStream(key).then(FullyDownloadedCachedFilesDataSource::CacheWriter).toFuture().get()
		}

		return innerDataSource.open(dataSpec)
	}

	override fun read(bytes: ByteArray, offset: Int, readLength: Int): Int =
		innerDataSource.read(bytes, offset, readLength).also { result ->
			cacheWriter?.let {
				if (result != C.RESULT_END_OF_INPUT) it.queueAndProcess(bytes, offset, result)
				else it.commit().also { cacheWriter = null }
			}
		}

	override fun getUri(): Uri? = innerDataSource.uri

	override fun getResponseHeaders(): Map<String, List<String>> = innerDataSource.responseHeaders

	override fun close() {
		innerDataSource.close()
		cacheWriter?.clear()
	}

	private class CacheWriter(private val cachedOutputStream: CacheOutputStream) {
		private val buffersToTransfer = ConcurrentLinkedQueue<Buffer>()
		private val activePromiseSync = Any()
		private val bufferSync = Any()

		@Volatile
		private var activePromise = Promise(cachedOutputStream)

		@Volatile
		private var workingBuffer = Buffer()

		fun queueAndProcess(bytes: ByteArray, offset: Int, length: Int) {
			synchronized(bufferSync) {
				workingBuffer.write(bytes, offset, length)
				if (workingBuffer.size < goodBufferSize) return

				buffersToTransfer.offer(workingBuffer)

				workingBuffer = Buffer()

				processQueue().ignore()
			}
		}

		private fun processQueue() : Promise<CacheOutputStream> = synchronized(activePromiseSync) {
			activePromise.eventually({
				if (it == null) return@eventually Promise.empty()

				val concatenatedBuffer = buffersToTransfer
					.drainQueue()
					.reduceOrNull { sink, source -> sink.also(source::readAll) }

				if (concatenatedBuffer == null || concatenatedBuffer.exhausted()) it.toPromise()
				else it.promiseTransfer(concatenatedBuffer)
			}, {
				logger.warn("An error occurred copying the buffer, closing the output stream", it)
				clear()
				Promise.empty()
			}).also { activePromise = it }
		}

		fun commit() {
			synchronized(activePromiseSync) {
				activePromise = activePromise
					.eventually {
						synchronized(bufferSync) {
							buffersToTransfer.offer(workingBuffer)
							processQueue()
						}
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
		override fun createDataSource(): DataSource = FullyDownloadedCachedFilesDataSource(
			httpDataSourceFactory.createDataSource(),
			cacheStreamSupplier
		)
	}

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(FullyDownloadedCachedFilesDataSource::class.java) }
		private const val goodBufferSize = 1024L * 1024L // 1MB
	}
}
