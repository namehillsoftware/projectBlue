package com.lasthopesoftware.bluewater.client.library.items.media.audio;

import android.content.Context;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

public class DiskFileCacheDataSourceFactory implements DataSource.Factory {

	private final DefaultHttpDataSourceFactory httpDataSourceFactory;
	private final ServiceFile serviceFile;
	private final DiskFileCache diskFileCache;

	public DiskFileCacheDataSourceFactory(Context context, DiskFileCache diskFileCache, TransferListener<? super DataSource> transferListener, Library library, ServiceFile serviceFile) {
		this.serviceFile = serviceFile;
		this.diskFileCache = diskFileCache;
		httpDataSourceFactory = new DefaultHttpDataSourceFactory(
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
