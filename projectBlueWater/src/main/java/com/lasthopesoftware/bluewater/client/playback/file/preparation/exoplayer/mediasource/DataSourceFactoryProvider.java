package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.lasthopesoftware.bluewater.client.library.items.media.audio.DiskFileCacheDataSourceFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.IoCommon;

public class DataSourceFactoryProvider {

	private final Context context;
	private final Library library;
	private final DiskFileCache diskFileCache;

	public DataSourceFactoryProvider(Context context, Library library, DiskFileCache diskFileCache) {
		this.context = context;
		this.library = library;
		this.diskFileCache = diskFileCache;
	}

	public DataSource.Factory getFactory(Uri uri, ServiceFile serviceFile, TransferListener<? super DataSource> transferListener) {
		return uri.getScheme().equalsIgnoreCase(IoCommon.FileUriScheme)
			? new FileDataSourceFactory(transferListener)
			: new DiskFileCacheDataSourceFactory(context, diskFileCache, transferListener, library, serviceFile);
	}
}
