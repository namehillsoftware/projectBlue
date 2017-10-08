package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.IoCommon;

public class DataSourceFactoryProvider {

	private final Context context;
	private final Library library;

	public DataSourceFactoryProvider(Context context, Library library) {
		this.context = context;
		this.library = library;
	}

	public DataSource.Factory getFactory(Uri uri, TransferListener<? super DataSource> transferListener) {
		return uri.getScheme().equalsIgnoreCase(IoCommon.FileUriScheme)
			? new FileDataSourceFactory(transferListener)
			: getNewCacheDataSourceFactory(transferListener);
	}

	private CacheDataSourceFactory getNewCacheDataSourceFactory(TransferListener<? super DataSource> transferListener) {
		DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(
			Util.getUserAgent(context, context.getString(R.string.app_name)),
			transferListener);

		final String authKey = library.getAuthKey();

		if (authKey != null && !authKey.isEmpty())
			httpDataSourceFactory.getDefaultRequestProperties().set("Authorization", "basic " + authKey);

		Cache cache = new SimpleCache(context.getCacheDir(), new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 10));
		return new CacheDataSourceFactory(cache, httpDataSourceFactory);
	}
}
