package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource;

import android.net.Uri;

import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

public class MediaSourceProvider implements SpawnMediaSources {

	private static final CreateAndHold<ExtractorsFactory> extractorsFactory = new Lazy<>(() -> Mp3Extractor.FACTORY);

	private final CreateAndHold<ExtractorMediaSource.Factory> lazyFileExtractorFactory = new AbstractSynchronousLazy<ExtractorMediaSource.Factory>() {
		@Override
		protected ExtractorMediaSource.Factory create() {
			final ExtractorMediaSource.Factory factory = new ExtractorMediaSource.Factory(new FileDataSourceFactory());
			factory.setExtractorsFactory(extractorsFactory.getObject());
			return factory;
		}
	};

	private final CreateAndHold<ExtractorMediaSource.Factory> lazyRemoteExtractorFactory;

	public MediaSourceProvider(Library library, ProvideHttpDataSourceFactory dataSourceFactoryProvider, Cache cache) {

		lazyRemoteExtractorFactory = new AbstractSynchronousLazy<ExtractorMediaSource.Factory>() {
			@Override
			protected ExtractorMediaSource.Factory create() {
				final HttpDataSource.Factory httpDataSourceFactory = dataSourceFactoryProvider.getHttpDataSourceFactory(library);

				final CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(
					cache,
					httpDataSourceFactory);

				final ExtractorMediaSource.Factory factory = new ExtractorMediaSource.Factory(cacheDataSourceFactory);
				factory.setLoadErrorHandlingPolicy(new DefaultLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy.DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE));
				factory.setExtractorsFactory(extractorsFactory.getObject());
				return factory;
			}
		};
	}

	@Override
	public MediaSource getNewMediaSource(Uri uri) {
		return getFactory(uri).createMediaSource(uri);
	}

	private ExtractorMediaSource.Factory getFactory(Uri uri) {
		return IoCommon.FileUriScheme.equalsIgnoreCase(uri.getScheme())
			? lazyFileExtractorFactory.getObject()
			: lazyRemoteExtractorFactory.getObject();
	}
}
