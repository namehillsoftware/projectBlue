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
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.toFuture
import com.lasthopesoftware.resources.uri.PathAndQuery
import com.namehillsoftware.handoff.promises.Promise
import okio.Buffer
import org.slf4j.LoggerFactory

class DiskFileCacheDataSource(
	private val innerDataSource: HttpDataSource,
	private val cacheStreamSupplier: ICacheStreamSupplier,
	private val cachedFilesProvider: ICachedFilesProvider
) : DataSource {
	private var buffer: Buffer? = null
	private lateinit var promisedOutputStream: Promise<CacheOutputStream>
	private lateinit var currentDataSpec: DataSpec

	override fun addTransferListener(transferListener: TransferListener) = innerDataSource.addTransferListener(transferListener)

	override fun open(dataSpec: DataSpec): Long {
		buffer = Buffer()

		val key = "${PathAndQuery.forUri(dataSpec.uri)}:${dataSpec.position}:${dataSpec.length}"
		val cachedFile = cachedFilesProvider.promiseCachedFile(key)
			.then {
				if (it == null)
					promisedOutputStream = cacheStreamSupplier.promiseCachedFileOutputStream(key)
				it
			}
			.toFuture()
			.get()

		currentDataSpec = if (cachedFile != null) dataSpec.buildUpon().setUri(cachedFile.fileName.toUri()).build() else dataSpec

		return innerDataSource.open(dataSpec)
	}

	override fun read(bytes: ByteArray, offset: Int, readLength: Int): Int {
		val result = innerDataSource.read(bytes, offset, readLength)
		val bufferToWrite = buffer ?: return result
		if (result != C.RESULT_END_OF_INPUT) {
			bufferToWrite.write(bytes, offset, result)
			if (bufferToWrite.size <= maxBufferSize) return result

			buffer = Buffer()
			promisedOutputStream = promisedOutputStream
				.eventually { cachedFileOutputStream ->
					val promisedWrite = cachedFileOutputStream?.promiseTransfer(bufferToWrite).keepPromise()
					promisedWrite.then(
						{ bufferToWrite.close() },
						{ e ->
							logger.warn("An error occurred storing the audio file", e)
							bufferToWrite.close()
							cachedFileOutputStream?.close()
						})
					promisedWrite
				}
			return result
		}

		val outputStream = if (bufferToWrite.size == 0L) promisedOutputStream else promisedOutputStream
			.eventually { cachedFileOutputStream ->
				val promisedWrite = cachedFileOutputStream?.promiseTransfer(bufferToWrite).keepPromise()
				promisedWrite.then(
					{ bufferToWrite.close() },
					{ e ->
						logger.warn("An error occurred storing the audio file", e)
						bufferToWrite.close()
						cachedFileOutputStream?.close()
					})
				promisedWrite
			}

		outputStream?.eventually { cachedFileOutputStream ->
			cachedFileOutputStream
				?.flush()
				?.eventually(
					{ os ->
						os.close()
						buffer?.close()
						os.commitToCache()
					}
				) { e ->
					logger.warn("An error occurred flushing the output stream", e)
					cachedFileOutputStream.close()
					buffer?.close()
					Promise.empty()
				}
				.keepPromise()
		}
		return result
	}

	override fun getUri(): Uri = currentDataSpec.uri

	override fun getResponseHeaders(): Map<String, List<String>> = innerDataSource.responseHeaders

	override fun close() {
		innerDataSource.close()
	}

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(DiskFileCacheDataSource::class.java) }
		private const val maxBufferSize = 1024L * 1024L // 1MB
	}
}
