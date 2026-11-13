package com.lasthopesoftware.bluewater.client.playback.exoplayer

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.HttpDataSource.HttpDataSourceException
import java.net.SocketException
import java.util.concurrent.atomic.AtomicLong

@OptIn(UnstableApi::class)
class RetryingExoPlayerDataSource(private val innerHttpDataSource: HttpDataSource) : HttpDataSource by innerHttpDataSource {
	/** [androidx.media3.datasource.DataSource.Factory] for [HttpPromiseClientDataSource] instances.  */
	@OptIn(UnstableApi::class)
	class Factory(private val innerDataSource: HttpDataSource.Factory) : HttpDataSource.Factory by innerDataSource {
		@UnstableApi
		override fun createDataSource(): RetryingExoPlayerDataSource {
			return RetryingExoPlayerDataSource(innerDataSource.createDataSource())
		}
	}

	private val totalBytesRead = AtomicLong()

	override fun open(dataSpec: DataSpec): Long {
		totalBytesRead.set(dataSpec.position)
		return innerHttpDataSource.open(dataSpec)
	}

	override fun read(bytes: ByteArray, offset: Int, maxBytesToRead: Int): Int {
		var attempts = 0
		do {
			attempts++
			try {
				return innerHttpDataSource.read(bytes, offset, maxBytesToRead).also { totalBytesRead.getAndAdd(it.toLong()) }
			} catch (dataSourceException: HttpDataSourceException) {
				when {
					attempts >= 2 -> throw dataSourceException
					dataSourceException.type != HttpDataSourceException.TYPE_READ -> throw dataSourceException
					dataSourceException.cause?.let { it !is SocketException || it.message != "Connection reset" } ?: true -> throw dataSourceException
					else -> {
						close()
						open(
							dataSourceException
								.dataSpec
								.subrange(totalBytesRead.get())
						)
					}
				}
			}
		} while (true)
	}
}
