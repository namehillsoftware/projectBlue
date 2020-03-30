package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource;

import android.net.Uri;

import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

public class MediaSourceProvider implements SpawnMediaSources {

	private static final CreateAndHold<ExtractorsFactory> extractorsFactory = new Lazy<>(() -> Mp3Extractor.FACTORY);

	private final CreateAndHold<ProgressiveMediaSource.Factory> lazyFileExtractorFactory = new AbstractSynchronousLazy<ProgressiveMediaSource.Factory>() {
		@Override
		protected ProgressiveMediaSource.Factory create() {
			return new ProgressiveMediaSource.Factory(new FileDataSourceFactory(), extractorsFactory.getObject());
		}
	};

	private final CreateAndHold<ProgressiveMediaSource.Factory> lazyRemoteExtractorFactory;

	public MediaSourceProvider(Library library, ProvideHttpDataSourceFactory dataSourceFactoryProvider, Cache cache) {

		lazyRemoteExtractorFactory = new AbstractSynchronousLazy<ProgressiveMediaSource.Factory>() {
			@Override
			protected ProgressiveMediaSource.Factory create() {
				final HttpDataSource.Factory httpDataSourceFactory = dataSourceFactoryProvider.getHttpDataSourceFactory(library);

				final CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(
					cache,
					httpDataSourceFactory);

				final ProgressiveMediaSource.Factory factory = new ProgressiveMediaSource.Factory(cacheDataSourceFactory, extractorsFactory.getObject());
				factory.setLoadErrorHandlingPolicy(new DefaultLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy.DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE));
				return factory;
			}
		};
	}

	@Override
	public MediaSource getNewMediaSource(Uri uri) {
		return getFactory(uri).createMediaSource(uri);
	}

	private ProgressiveMediaSource.Factory getFactory(Uri uri) {
		return IoCommon.FileUriScheme.equalsIgnoreCase(uri.getScheme())
			? lazyFileExtractorFactory.getObject()
			: lazyRemoteExtractorFactory.getObject();
	}
}
