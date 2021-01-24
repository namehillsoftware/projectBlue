package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import android.net.Uri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.shared.IoCommon

class MediaSourceProvider(library: Library, dataSourceFactoryProvider: ProvideHttpDataSourceFactory, cache: Cache) : SpawnMediaSources {

	companion object {
		private val extractorsFactory = lazy { Mp3Extractor.FACTORY }
	}

	private val lazyFileExtractorFactory = lazy {
		ProgressiveMediaSource.Factory(FileDataSource.Factory(), extractorsFactory.value)
	}

	private val lazyRemoteExtractorFactory = lazy {
		val httpDataSourceFactory = dataSourceFactoryProvider.getHttpDataSourceFactory(library)
		val cacheDataSourceFactory = CacheDataSource.Factory()
			.setCache(cache)
			.setUpstreamDataSourceFactory(httpDataSourceFactory)

		val factory = ProgressiveMediaSource.Factory(cacheDataSourceFactory, extractorsFactory.value)
		factory.setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy.DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE))
	}

	override fun getNewMediaSource(uri: Uri, startAt: Long): MediaSource =
		getFactory(uri)
			.createMediaSource(MediaItem.Builder().setUri(uri).setClipStartPositionMs(startAt).build())

	private fun getFactory(uri: Uri): ProgressiveMediaSource.Factory =
		if (IoCommon.FileUriScheme.equals(uri.scheme, ignoreCase = true)) lazyFileExtractorFactory.value
		else lazyRemoteExtractorFactory.value
}
