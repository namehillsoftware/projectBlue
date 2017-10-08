package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource.DataSourceFactoryProvider;
import com.lasthopesoftware.messenger.promises.Promise;

final class ExoPlayerPlaybackPreparer implements IPlaybackPreparer {

	private final Context context;
	private final IFileUriProvider fileUriProvider;
	private final DataSourceFactoryProvider dataSourceFactoryProvider;

	ExoPlayerPlaybackPreparer(Context context, DataSourceFactoryProvider dataSourceFactoryProvider, IFileUriProvider fileUriProvider) {
		this.context = context;
		this.fileUriProvider = fileUriProvider;
		this.dataSourceFactoryProvider = dataSourceFactoryProvider;
	}

	@Override
	public Promise<PreparedPlaybackFile> promisePreparedPlaybackHandler(ServiceFile serviceFile, int preparedAt) {
		return
			fileUriProvider
				.getFileUri(serviceFile)
				.eventually(new ExoPlayerPreparerTask(context, dataSourceFactoryProvider, preparedAt));
	}
}
