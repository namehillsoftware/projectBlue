package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.DiskFileCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.access.CachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.disk.AndroidDiskCacheDirectoryProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.persistence.DiskFileAccessTimeUpdater
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.persistence.DiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.stream.supplier.DiskFileCacheStreamSupplier
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.caching.AudioCacheConfiguration
import com.lasthopesoftware.bluewater.client.playback.caching.DiskFileCacheDataSource
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.IoCommon
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class MediaSourceProvider(
	library: Library,
	dataSourceFactoryProvider: ProvideHttpDataSourceFactory,
	private val applicationSettings: HoldApplicationSettings,
	context: Context,
) : SpawnMediaSources, AutoCloseable {

	companion object {
		private val extractorsFactory by lazy { Mp3Extractor.FACTORY }
	}

	private val lazyFileExtractorFactory by lazy {
		ProgressiveMediaSource.Factory(FileDataSource.Factory(), extractorsFactory).toPromise()
	}

	private val diskCachedDirectoryProvider by lazy { AndroidDiskCacheDirectoryProvider(context) }

	private val cacheConfiguration by lazy { AudioCacheConfiguration(library) }

	private val cachedFilesProvider by lazy { CachedFilesProvider(context, cacheConfiguration) }

	private val diskFileAccessTimeUpdater by lazy { DiskFileAccessTimeUpdater(context) }

	private val cacheStreamSupplier by lazy {
		DiskFileCacheStreamSupplier(
			diskCachedDirectoryProvider,
			cacheConfiguration,
			DiskFileCachePersistence(
				context,
				diskCachedDirectoryProvider,
				cacheConfiguration,
				cachedFilesProvider,
				diskFileAccessTimeUpdater
			),
			cachedFilesProvider
		)
	}

	private val audioCache by lazy { DiskFileCache(context, diskCachedDirectoryProvider, cacheConfiguration, cacheStreamSupplier, cachedFilesProvider, diskFileAccessTimeUpdater) }

	private val remoteExtractorCustomCacheFactory by lazy {
		val httpDataSourceFactory = dataSourceFactoryProvider.getHttpDataSourceFactory(library)
		val cacheDataSourceFactory = DiskFileCacheDataSource.Factory(httpDataSourceFactory, cacheStreamSupplier, audioCache)

		val factory = ProgressiveMediaSource.Factory(cacheDataSourceFactory, extractorsFactory)
		factory.setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy.DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE))
	}

	private val simpleCache = lazy {
		val cacheDirectory = diskCachedDirectoryProvider.getDiskCacheDirectory(cacheConfiguration)
		val cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheConfiguration.maxSize)
		SimpleCache(cacheDirectory, cacheEvictor)
	}

	private val remoteExtractorExoPlayerCacheFactory by lazy {
		val httpDataSourceFactory = dataSourceFactoryProvider.getHttpDataSourceFactory(library)
		val cacheDataSourceFactory = CacheDataSource.Factory()
			.setCache(simpleCache.value)
			.setUpstreamDataSourceFactory(httpDataSourceFactory)

		val factory = ProgressiveMediaSource.Factory(cacheDataSourceFactory, extractorsFactory)
		factory.setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy.DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE))
	}

	override fun promiseNewMediaSource(uri: Uri): Promise<MediaSource> =
		getFactory(uri).then { f -> f.createMediaSource(MediaItem.Builder().setUri(uri).build()) }

	private fun getFactory(uri: Uri): Promise<ProgressiveMediaSource.Factory> =
		if (IoCommon.FileUriScheme.equals(uri.scheme, ignoreCase = true)) lazyFileExtractorFactory
		else applicationSettings.promiseApplicationSettings().then { s ->
			if (s.isUsingCustomCaching) remoteExtractorCustomCacheFactory
			else remoteExtractorExoPlayerCacheFactory
		}

	override fun close() {
		if (simpleCache.isInitialized()) simpleCache.value.release()
	}
}
