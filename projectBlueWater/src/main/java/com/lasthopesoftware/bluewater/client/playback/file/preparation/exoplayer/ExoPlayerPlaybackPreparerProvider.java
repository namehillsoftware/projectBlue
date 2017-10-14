package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.audio.AudioCacheConfiguration;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.access.CachedFilesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.DiskFileAccessTimeUpdater;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.DiskFileCachePersistence;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource.DataSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.queues.IPreparedPlaybackQueueConfiguration;


public class ExoPlayerPlaybackPreparerProvider implements IPlaybackPreparerProvider, IPreparedPlaybackQueueConfiguration {

	private final Context context;
	private final IFileUriProvider fileUriProvider;
	private final DataSourceFactoryProvider dataSourceFactoryProvder;
	private final DiskFileCache diskFileCache;

	public ExoPlayerPlaybackPreparerProvider(Context context, IFileUriProvider fileUriProvider, Library library) {
		this.context = context;
		this.fileUriProvider = fileUriProvider;

		final AudioCacheConfiguration audioCacheConfiguration = new AudioCacheConfiguration(library);
		final CachedFilesProvider cachedFilesProvider = new CachedFilesProvider(context, audioCacheConfiguration);
		final DiskFileAccessTimeUpdater diskFileAccessTimeUpdater = new DiskFileAccessTimeUpdater(context);
		diskFileCache = new DiskFileCache(
			context,
			audioCacheConfiguration,
			new DiskFileCachePersistence(
				context,
				audioCacheConfiguration,
				cachedFilesProvider,
				diskFileAccessTimeUpdater),
			cachedFilesProvider,
			diskFileAccessTimeUpdater);

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
