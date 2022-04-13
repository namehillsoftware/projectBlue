package com.lasthopesoftware.bluewater.client.playback.caching

import android.net.Uri
import androidx.core.net.toUri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.TransferListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.ICache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.CacheOutputStream
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.supplier.ICacheStreamSupplier
import com.lasthopesoftware.bluewater.shared.drainQueue
import com.lasthopesoftware.bluewater.shared.promises.NoopResponse.Companion.ignore
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.toFuture
import com.lasthopesoftware.resources.uri.PathAndQuery
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.internal.closeQuietly
import okio.Buffer
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentLinkedQueue

class DiskFileCacheDataSource(
	private val innerDataSource: HttpDataSource,
	private val cacheStreamSupplier: ICacheStreamSupplier,
	private val cachedFilesProvider: ICache
) : DataSource {
	private var inputStream: InputStream? = null
	private var cacheWriter: CacheWriter? = null
	private lateinit var currentDataSpec: DataSpec

	override fun addTransferListener(transferListener: TransferListener) = innerDataSource.addTransferListener(transferListener)

	override fun open(dataSpec: DataSpec): Long {
		cacheWriter?.commit()?.also { cacheWriter = null }
		inputStream?.close()?.also { inputStream = null }

		val key = "${PathAndQuery.forUri(dataSpec.uri)}:${dataSpec.position}:${dataSpec.length}"
		val cachedFile = cachedFilesProvider
			.promiseCachedFile(key)
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

		currentDataSpec = dataSpec
		return cachedFile?.let { cf ->
			currentDataSpec = dataSpec.buildUpon()
				.setUri(cf.toUri())
				.setLength(cf.length())
				.build()
			inputStream = FileInputStream(cf)
			cf.length()
		} ?: innerDataSource.open(dataSpec)
	}

	override fun read(bytes: ByteArray, offset: Int, readLength: Int): Int =
		inputStream?.read(bytes, offset, readLength)?.let { if (it != 0) it else C.LENGTH_UNSET }
			?: innerDataSource.read(bytes, offset, readLength).also { result ->
				cacheWriter?.apply {
					if (result != C.RESULT_END_OF_INPUT) queueAndProcess(bytes, offset, result)
					else commit().also { cacheWriter = null }
				}
			}

	override fun getUri(): Uri = currentDataSpec.uri

	override fun getResponseHeaders(): Map<String, List<String>> = innerDataSource.responseHeaders

	override fun close() {
		innerDataSource.close()
		inputStream?.close()
		cacheWriter?.clear()
	}

	private class CacheWriter(private val cachedOutputStream: CacheOutputStream) {
		private val buffersToTransfer = ConcurrentLinkedQueue<Buffer>()
		private val activePromiseSync = Any()

		@Volatile
		private var activePromise = Promise(cachedOutputStream)

		@Volatile
		private var workingBuffer = Buffer()

		@Synchronized
		fun queueAndProcess(bytes: ByteArray, offset: Int, length: Int) {
			workingBuffer.write(bytes, offset, length)
			if (workingBuffer.size < goodBufferSize) return

			buffersToTransfer.offer(workingBuffer)

			workingBuffer = Buffer()

			processQueue().ignore()
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
						buffersToTransfer.offer(workingBuffer)
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
		private val cacheStreamSupplier: ICacheStreamSupplier,
	 	private val cachedFilesProvider: ICache) : DataSource.Factory {
		override fun createDataSource(): DataSource = DiskFileCacheDataSource(
			httpDataSourceFactory.createDataSource(),
			cacheStreamSupplier,
			cachedFilesProvider
		)
	}

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(DiskFileCacheDataSource::class.java) }
		private const val goodBufferSize = 1024L * 1024L // 1MB
	}
}
