package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource.DataSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.queues.IPreparedPlaybackQueueConfiguration;


public class ExoPlayerPlaybackPreparerProvider implements IPlaybackPreparerProvider, IPreparedPlaybackQueueConfiguration {

	private static final String musicCacheName = "music";
	private static final long maxFileCacheSize = 5368709000L; // 5GB

	private final Context context;
	private final IFileUriProvider fileUriProvider;
	private final DiskFileCache diskFileCache;
	private final DataSourceFactoryProvider dataSourceFactoryProvder;

	public ExoPlayerPlaybackPreparerProvider(Context context, IFileUriProvider fileUriProvider, Library library) {
		this.context = context;
		this.fileUriProvider = fileUriProvider;
		diskFileCache = new DiskFileCache(context, library, musicCacheName, maxFileCacheSize);
		dataSourceFactoryProvder = new DataSourceFactoryProvider(context, library, diskFileCache);
	}

	@Override
	public int getMaxQueueSize() {
		return 5;
	}

	@Override
	public IPlaybackPreparer providePlaybackPreparer() {
		return new ExoPlayerPlaybackPreparer(context, dataSourceFactoryProvder, diskFileCache, fileUriProvider);
	}
}
