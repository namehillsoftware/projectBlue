package com.lasthopesoftware.bluewater.client.playback.caching.mediasource

import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.configuration.IDiskFileCacheConfiguration
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.disk.IDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ProvideHttpDataSourceFactory

class SimpleCacheSourceFactory(
	dataSourceFactoryProvider: ProvideHttpDataSourceFactory,
	diskCachedDirectoryProvider: IDiskCacheDirectoryProvider,
	cacheConfiguration: IDiskFileCacheConfiguration
) : AutoCloseable {

	private val simpleCache = lazy {
		val cacheDirectory = diskCachedDirectoryProvider.getDiskCacheDirectory(cacheConfiguration)
		val cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheConfiguration.maxSize)
		SimpleCache(cacheDirectory, cacheEvictor)
	}

	private val remoteExtractorExoPlayerCacheFactory by lazy {
		val httpDataSourceFactory = dataSourceFactoryProvider.getHttpDataSourceFactory()
		CacheDataSource.Factory()
			.setCache(simpleCache.value)
			.setUpstreamDataSourceFactory(httpDataSourceFactory)
	}

	fun getSimpleCacheFactory() = remoteExtractorExoPlayerCacheFactory

	override fun close() {
		if (simpleCache.isInitialized()) simpleCache.value.release()
	}
}
