package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import android.net.Uri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.FileDataSource
import com.lasthopesoftware.bluewater.client.playback.caching.datasource.DiskFileCacheSourceFactory
import com.lasthopesoftware.bluewater.shared.IoCommon
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class MediaSourceProvider(
	diskFileCacheSourceFactory: DiskFileCacheSourceFactory,
) : SpawnMediaSources {

	companion object {
		private val extractorsFactory by lazy { Mp3Extractor.FACTORY }
	}

	private val lazyFileExtractorFactory by lazy {
		ProgressiveMediaSource.Factory(FileDataSource.Factory(), extractorsFactory)
	}

	private val remoteExtractorCustomCacheFactory by lazy {
		val cacheDataSourceFactory = diskFileCacheSourceFactory.getDiskFileCacheSource()

		val factory = ProgressiveMediaSource.Factory(cacheDataSourceFactory, extractorsFactory)
		factory.setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy.DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE))
	}

	override fun promiseNewMediaSource(uri: Uri): Promise<MediaSource> =
		getFactory(uri).createMediaSource(MediaItem.Builder().setUri(uri).build()).toPromise()

	private fun getFactory(uri: Uri) =
		if (IoCommon.FileUriScheme.equals(uri.scheme, ignoreCase = true)) lazyFileExtractorFactory
		else remoteExtractorCustomCacheFactory
}
