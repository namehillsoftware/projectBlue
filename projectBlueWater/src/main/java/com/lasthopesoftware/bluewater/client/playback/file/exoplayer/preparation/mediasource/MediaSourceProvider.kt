package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import android.net.Uri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.FileDataSource
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.caching.datasource.DiskFileCacheSourceFactory
import com.lasthopesoftware.bluewater.shared.IoCommon
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.ConcurrentHashMap

class MediaSourceProvider(
	private val diskFileCacheSourceFactory: DiskFileCacheSourceFactory,
) : SpawnMediaSources {

	companion object {
		private val extractorsFactory by lazy { Mp3Extractor.FACTORY }
	}

	private val lazyFileExtractorFactory by lazy {
		ProgressiveMediaSource.Factory(FileDataSource.Factory(), extractorsFactory)
	}

	// ExoPlayer doesn't give us a good way to bundle a library ID in with a DataSpec request for th cache, so we will
	// instead create a cache factory per library ID, and cache the cache factories. This will end up being a finite amount
	// of factories (likely just 1), so I'm not overly concerned about resource usage.
	private val remoteExtractorCustomCacheFactories = ConcurrentHashMap<LibraryId, MediaSource.Factory>()

	override fun promiseNewMediaSource(libraryId: LibraryId, uri: Uri): Promise<MediaSource> =
		getFactory(libraryId, uri).createMediaSource(MediaItem.Builder().setUri(uri).build()).toPromise()

	private fun getFactory(libraryId: LibraryId, uri: Uri): MediaSource.Factory =
		if (IoCommon.FileUriScheme.equals(uri.scheme, ignoreCase = true)) lazyFileExtractorFactory
		else remoteExtractorCustomCacheFactories.getOrPut(libraryId) {
			val cacheDataSourceFactory = diskFileCacheSourceFactory.getDiskFileCacheSource(libraryId)

			val factory = ProgressiveMediaSource.Factory(cacheDataSourceFactory, extractorsFactory)
			factory.setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy.DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE))
		}
}
