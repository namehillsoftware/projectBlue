package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer;

import android.content.Context;
import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource.DataSourceFactoryProvider;
import com.lasthopesoftware.messenger.promises.Promise;

final class ExoPlayerPlaybackPreparer implements IPlaybackPreparer {

	private final Context context;
	private final DiskFileCache diskFileCache;
	private final IFileUriProvider fileUriProvider;
	private final DataSourceFactoryProvider dataSourceFactoryProvider;

	ExoPlayerPlaybackPreparer(Context context, DataSourceFactoryProvider dataSourceFactoryProvider, DiskFileCache diskFileCache, IFileUriProvider fileUriProvider) {
		this.context = context;
		this.diskFileCache = diskFileCache;
		this.fileUriProvider = fileUriProvider;
		this.dataSourceFactoryProvider = dataSourceFactoryProvider;
	}

	@Override
	public Promise<PreparedPlaybackFile> promisePreparedPlaybackHandler(ServiceFile serviceFile, int preparedAt) {
		return diskFileCache.promiseCachedFile(String.valueOf(serviceFile.getKey()))
			.eventually(file -> file != null
				? new Promise<>(Uri.fromFile(file))
				: fileUriProvider.getFileUri(serviceFile))
			.eventually(new ExoPlayerPreparerTask(context, dataSourceFactoryProvider, preparedAt));
	}
}
