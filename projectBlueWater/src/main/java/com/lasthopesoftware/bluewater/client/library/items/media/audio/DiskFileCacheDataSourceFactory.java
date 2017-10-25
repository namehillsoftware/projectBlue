package com.lasthopesoftware.bluewater.client.library.items.media.audio;

import android.content.Context;

import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.ILazy;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class DiskFileCacheDataSourceFactory implements DataSource.Factory {

	private static final ILazy<OkHttpClient> okHttpClient = new AbstractSynchronousLazy<OkHttpClient>() {

		@Override
		protected OkHttpClient initialize() throws Exception {
			return new OkHttpClient.Builder()
				.readTimeout(1, TimeUnit.MINUTES)
				.build();
		}
	};

	private final OkHttpDataSourceFactory httpDataSourceFactory;
	private final ServiceFile serviceFile;
	private final DiskFileCache diskFileCache;

	public DiskFileCacheDataSourceFactory(Context context, DiskFileCache diskFileCache, TransferListener<? super DataSource> transferListener, Library library, ServiceFile serviceFile) {
		this.serviceFile = serviceFile;
		this.diskFileCache = diskFileCache;
		httpDataSourceFactory = new OkHttpDataSourceFactory(
			okHttpClient.getObject(),
			Util.getUserAgent(context, context.getString(R.string.app_name)),
			transferListener);

		final String authKey = library.getAuthKey();

		if (authKey != null && !authKey.isEmpty())
			httpDataSourceFactory.getDefaultRequestProperties().set("Authorization", "basic " + authKey);
	}

	@Override
	public DataSource createDataSource() {
		return new DiskFileCacheDataSource(httpDataSourceFactory.createDataSource(), serviceFile, diskFileCache);
	}
}
