package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource.DataSourceFactoryProvider;
import com.lasthopesoftware.messenger.promises.Promise;

final class ExoPlayerPlaybackPreparer implements IPlaybackPreparer {

	private final Context context;
	private final DataSourceFactoryProvider dataSourceFactoryProvider;
	private final TrackSelector trackSelector;
	private final LoadControl loadControl;
	private final RenderersFactory rendersFactory;
	private final ExtractorsFactory extractorsFactory;
	private final Handler handler;
	private final DiskFileCache diskFileCache;
	private final IFileUriProvider fileUriProvider;

	ExoPlayerPlaybackPreparer(Context context, DataSourceFactoryProvider dataSourceFactoryProvider, TrackSelector trackSelector, LoadControl loadControl, RenderersFactory rendersFactory, ExtractorsFactory extractorsFactory, Handler handler, DiskFileCache diskFileCache, IFileUriProvider fileUriProvider) {
		this.context = context;
		this.trackSelector = trackSelector;
		this.loadControl = loadControl;
		this.rendersFactory = rendersFactory;
		this.extractorsFactory = extractorsFactory;
		this.handler = handler;
		this.diskFileCache = diskFileCache;
		this.fileUriProvider = fileUriProvider;
		this.dataSourceFactoryProvider = dataSourceFactoryProvider;
	}

	@Override
	public Promise<PreparedPlaybackFile> promisePreparedPlaybackHandler(ServiceFile serviceFile, long preparedAt) {
		return diskFileCache.promiseCachedFile(String.valueOf(serviceFile.getKey()))
			.eventually(file -> file != null
				? new Promise<>(Uri.fromFile(file))
				: fileUriProvider.getFileUri(serviceFile))
			.eventually(
				new ExoPlayerPreparerTask(
					dataSourceFactoryProvider,
					trackSelector,
					loadControl,
					rendersFactory,
					extractorsFactory,
					handler,
					serviceFile,
					preparedAt));
	}
}
