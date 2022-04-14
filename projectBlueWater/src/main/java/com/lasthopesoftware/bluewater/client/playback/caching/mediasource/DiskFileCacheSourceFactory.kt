package com.lasthopesoftware.bluewater.client.playback.caching.mediasource

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.CacheFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.supplier.SupplyCacheStreams
import com.lasthopesoftware.bluewater.client.playback.caching.DiskFileCacheDataSource
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ProvideHttpDataSourceFactory

class DiskFileCacheSourceFactory(
	dataSourceFactoryProvider: ProvideHttpDataSourceFactory,
	cacheStreamSupplier: SupplyCacheStreams,
	cache: CacheFiles
) {

	private val remoteExtractorCustomCacheFactory by lazy {
		val httpDataSourceFactory = dataSourceFactoryProvider.getHttpDataSourceFactory()
		DiskFileCacheDataSource.Factory(httpDataSourceFactory, cacheStreamSupplier, cache)
	}

	fun getDiskFileCacheSource() = remoteExtractorCustomCacheFactory
}
