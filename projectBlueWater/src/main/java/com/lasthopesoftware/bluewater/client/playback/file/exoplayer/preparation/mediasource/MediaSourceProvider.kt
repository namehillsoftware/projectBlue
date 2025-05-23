package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ContentDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.resources.uri.IoCommon
import com.namehillsoftware.handoff.promises.Promise

@UnstableApi class MediaSourceProvider(
	private val context: Context,
	private val remoteDataSourceFactory: ProvideRemoteDataSourceFactory
) : SpawnMediaSources {

	companion object {
		private val extractorsFactory by lazy { Mp3Extractor.FACTORY }

		private val promisedFileExtractorFactory by lazy {
			Promise<MediaSource.Factory>(ProgressiveMediaSource.Factory(FileDataSource.Factory(), extractorsFactory))
		}
	}

	private val promisedContentExtractorFactory by lazy {
		Promise<MediaSource.Factory>(ProgressiveMediaSource.Factory(ContentDataSourceFactory(context), DefaultExtractorsFactory()))
	}

	override fun promiseNewMediaSource(libraryId: LibraryId, uri: Uri): Promise<MediaSource> =
		getFactory(libraryId, uri).cancelBackThen { it, _ -> it.createMediaSource(MediaItem.Builder().setUri(uri).build()) }

	private fun getFactory(libraryId: LibraryId, uri: Uri): Promise<MediaSource.Factory> =
		when {
			IoCommon.fileUriScheme.equals(uri.scheme, ignoreCase = true) -> promisedFileExtractorFactory
			IoCommon.contentUriScheme.equals(uri.scheme, ignoreCase = true) -> promisedContentExtractorFactory
			else -> remoteDataSourceFactory
				.promiseRemoteDataSourceFactory(libraryId)
				.cancelBackThen { cacheDataSourceFactory, _ ->
					val factory = ProgressiveMediaSource.Factory(cacheDataSourceFactory, extractorsFactory)
					factory.setLoadErrorHandlingPolicy(
						DefaultLoadErrorHandlingPolicy(
							DefaultLoadErrorHandlingPolicy.DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE
						)
					)
				}
		}

	private class ContentDataSourceFactory(private val context: Context) : DataSource.Factory {
		override fun createDataSource(): DataSource = ContentDataSource(context)
	}
}
