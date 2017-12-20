package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer;

import android.net.Uri;
import android.os.Handler;

import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource.DataSourceFactoryProvider;
import com.namehillsoftware.handoff.promises.Promise;

final class ExoPlayerPlaybackPreparer implements PlayableFilePreparationSource {

	private final DataSourceFactoryProvider dataSourceFactoryProvider;
	private final TrackSelector trackSelector;
	private final LoadControl loadControl;
	private final RenderersFactory rendersFactory;
	private final ExtractorsFactory extractorsFactory;
	private final Handler handler;
	private final DiskFileCache diskFileCache;
	private final IFileUriProvider fileUriProvider;

	ExoPlayerPlaybackPreparer(DataSourceFactoryProvider dataSourceFactoryProvider, TrackSelector trackSelector, LoadControl loadControl, RenderersFactory rendersFactory, ExtractorsFactory extractorsFactory, Handler handler, DiskFileCache diskFileCache, IFileUriProvider fileUriProvider) {
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
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
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
