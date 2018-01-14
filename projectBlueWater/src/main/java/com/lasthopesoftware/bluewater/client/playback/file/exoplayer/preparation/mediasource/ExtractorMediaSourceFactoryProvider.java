package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.audio.DiskFileCacheDataSourceFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.supplier.ICacheStreamSupplier;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

public class ExtractorMediaSourceFactoryProvider {

	private final Context context;
	private final Library library;
	private final ICacheStreamSupplier cacheStreamSupplier;

	private final CreateAndHold<ExtractorMediaSource.Factory> lazyFileExtractorFactory = new AbstractSynchronousLazy<ExtractorMediaSource.Factory>() {
		@Override
		protected ExtractorMediaSource.Factory create() throws Throwable {
			final ExtractorMediaSource.Factory factory = new ExtractorMediaSource.Factory(new FileDataSourceFactory());
			factory.setMinLoadableRetryCount(ExtractorMediaSource.DEFAULT_MIN_LOADABLE_RETRY_COUNT_LIVE);
			return factory;
		}
	};

	private final CreateAndHold<ExtractorMediaSource.Factory> lazyRemoteExtractorFactory = new AbstractSynchronousLazy<ExtractorMediaSource.Factory>() {
		@Override
		protected ExtractorMediaSource.Factory create() throws Throwable {
			final ExtractorMediaSource.Factory factory = new ExtractorMediaSource.Factory(new DiskFileCacheDataSourceFactory(context, cacheStreamSupplier, library));
			factory.setMinLoadableRetryCount(ExtractorMediaSource.DEFAULT_MIN_LOADABLE_RETRY_COUNT_LIVE);
			return factory;
		}
	};

	public ExtractorMediaSourceFactoryProvider(Context context, Library library, ICacheStreamSupplier cacheStreamSupplier) {
		this.context = context;
		this.library = library;
		this.cacheStreamSupplier = cacheStreamSupplier;
	}

	public ExtractorMediaSource.Factory getFactory(Uri uri) {
		return uri.getScheme().equalsIgnoreCase(IoCommon.FileUriScheme)
			? lazyFileExtractorFactory.getObject()
			: lazyRemoteExtractorFactory.getObject();
	}
}
