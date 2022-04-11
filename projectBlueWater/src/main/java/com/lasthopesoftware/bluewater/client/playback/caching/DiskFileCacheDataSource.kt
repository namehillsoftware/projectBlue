package com.lasthopesoftware.bluewater.client.playback.caching

import android.net.Uri
import androidx.core.net.toUri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.TransferListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.access.ICachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.supplier.ICacheStreamSupplier
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.toFuture
import com.lasthopesoftware.resources.uri.PathAndQuery
import com.namehillsoftware.handoff.promises.Promise
import okio.Buffer
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue

class DiskFileCacheDataSource(
	private val innerDataSource: HttpDataSource,
	private val cacheStreamSupplier: ICacheStreamSupplier,
	private val cachedFilesProvider: ICachedFilesProvider
) : DataSource {
	private var cacheWriter: CacheWriter? = null
	private lateinit var currentDataSpec: DataSpec

	override fun addTransferListener(transferListener: TransferListener) = innerDataSource.addTransferListener(transferListener)

	override fun open(dataSpec: DataSpec): Long {
		val key = "${PathAndQuery.forUri(dataSpec.uri)}:${dataSpec.position}:${dataSpec.length}"
		val cachedFile = cachedFilesProvider.promiseCachedFile(key)
			.eventually {
				it?.toPromise() ?: cacheStreamSupplier
					.promiseCachedFileOutputStream(key)
					.then { os ->
						cacheWriter = CacheWriter(os)
						it
					}
			}
			.toFuture()
			.get()

		currentDataSpec = cachedFile?.fileName?.toUri()?.let { dataSpec.buildUpon().setUri(it).build() } ?: dataSpec
		return innerDataSource.open(dataSpec)
	}

	override fun read(bytes: ByteArray, offset: Int, readLength: Int): Int {
		val result = innerDataSource.read(bytes, offset, readLength)
		if (result == C.RESULT_END_OF_INPUT || result < readLength) cacheWriter?.commit()
		else cacheWriter?.queueAndProcess(bytes, offset, result)
		return result
	}

	override fun getUri(): Uri = currentDataSpec.uri

	override fun getResponseHeaders(): Map<String, List<String>> = innerDataSource.responseHeaders

	override fun close() {
		innerDataSource.close()
	}

	private class CacheWriter(private val cachedOutputStream: CacheOutputStream) {
		private val buffers = ConcurrentLinkedQueue<Buffer>()
		private val activePromiseSync = Any()
		private var activePromise = Unit.toPromise()

		fun queueAndProcess(bytes: ByteArray, offset: Int, length: Int) {
			buffers.offer(Buffer().apply { write(bytes, offset, length) })

			processQueue()
		}

		private fun processQueue() : Promise<Unit> = synchronized(activePromiseSync) {
			activePromise.eventually {
				buffers.poll()
					?.let { buffer ->
						cachedOutputStream
							.promiseTransfer(buffer)
							.apply {
								excuse {
									logger.warn("An error occurred copying the buffer, closing the output stream", it)
									cachedOutputStream.close()
								}
							}
							.eventually { processQueue() }
					}
					?: Unit.toPromise()
			}.also { activePromise = it }
		}

		fun commit() {
			synchronized(activePromiseSync) {
				activePromise
					.eventually { cachedOutputStream.flush() }
					.eventually { cachedOutputStream.commitToCache() }
					.must { cachedOutputStream.close() }
			}
		}
	}

	class Factory(
		private val httpDataSourceFactory: HttpDataSource.Factory,
		private val cacheStreamSupplier: ICacheStreamSupplier,
	 	private val cachedFilesProvider: ICachedFilesProvider) : DataSource.Factory {
		override fun createDataSource(): DataSource = DiskFileCacheDataSource(
			httpDataSourceFactory.createDataSource(),
			cacheStreamSupplier,
			cachedFilesProvider
		)
	}

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(DiskFileCacheDataSource::class.java) }
		private const val maxBufferSize = 1024L * 1024L // 1MB
	}
}
