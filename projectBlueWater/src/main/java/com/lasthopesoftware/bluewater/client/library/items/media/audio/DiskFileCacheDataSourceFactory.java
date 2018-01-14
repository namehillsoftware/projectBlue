package com.lasthopesoftware.bluewater.client.library.items.media.audio;

import android.content.Context;

import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.Util;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.supplier.ICacheStreamSupplier;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class DiskFileCacheDataSourceFactory implements DataSource.Factory {

	private static final CreateAndHold<OkHttpClient> okHttpClient = new AbstractSynchronousLazy<OkHttpClient>() {

		@Override
		protected OkHttpClient create() throws Exception {
			return new OkHttpClient.Builder()
				.readTimeout(45, TimeUnit.SECONDS)
				.retryOnConnectionFailure(false)
				.build();
		}
	};

	private final OkHttpDataSourceFactory httpDataSourceFactory;
	private final ICacheStreamSupplier cacheStreamSupplier;
	private final ServiceFile serviceFile;

	public DiskFileCacheDataSourceFactory(Context context, ICacheStreamSupplier cacheStreamSupplier, Library library, ServiceFile serviceFile) {
		this.cacheStreamSupplier = cacheStreamSupplier;
		this.serviceFile = serviceFile;
		httpDataSourceFactory = new OkHttpDataSourceFactory(
			okHttpClient.getObject(),
			Util.getUserAgent(context, context.getString(R.string.app_name)),
			null);

		final String authKey = library.getAuthKey();

		if (authKey != null && !authKey.isEmpty())
			httpDataSourceFactory.getDefaultRequestProperties().set("Authorization", "basic " + authKey);
	}

	@Override
	public DataSource createDataSource() {
		return new DiskFileCacheDataSource(httpDataSourceFactory.createDataSource(), serviceFile, cacheStreamSupplier);
	}
}
