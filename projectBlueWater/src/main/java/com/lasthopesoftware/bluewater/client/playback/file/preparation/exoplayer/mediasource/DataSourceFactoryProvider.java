package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.ILazy;
import com.namehillsoftware.lazyj.Lazy;

class DataSourceFactoryProvider {
	private final ILazy<FileDataSourceFactory> lazyFileDataSourceFactory = new Lazy<>(FileDataSourceFactory::new);
	private final ILazy<CacheDataSourceFactory> lazyCacheDataSourceFactory;

	public DataSourceFactoryProvider(Context context, Library library) {
		lazyCacheDataSourceFactory = new AbstractSynchronousLazy<CacheDataSourceFactory>() {
			@Override
			protected CacheDataSourceFactory initialize() throws Exception {
				DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(context.getString(R.string.app_name));
				final String authKey = library.getAuthKey();

				if (authKey != null && !authKey.isEmpty())
					httpDataSourceFactory.getDefaultRequestProperties().set("Authorization", "basic " + authKey);

				Cache cache = new SimpleCache(context.getCacheDir(), new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 10));
				return new CacheDataSourceFactory(cache, httpDataSourceFactory);
			}
		};
	}

	DataSource.Factory getFactory(Uri uri) {
		return uri.getScheme().equalsIgnoreCase(IoCommon.FileUriScheme)
			? lazyFileDataSourceFactory.getObject()
			: lazyCacheDataSourceFactory.getObject();
	}
}
