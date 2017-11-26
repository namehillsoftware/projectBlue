package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.lasthopesoftware.bluewater.client.library.items.media.audio.AudioCacheConfiguration;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.access.CachedFilesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.DiskFileAccessTimeUpdater;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.DiskFileCachePersistence;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource.DataSourceFactoryProvider;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.joda.time.Minutes;


public class ExoPlayerPlaybackPreparerProvider implements PlaybackEngine {

	private static final CreateAndHold<Integer> maxBufferMs = new Lazy<>(() -> (int) Minutes.minutes(5).toStandardDuration().getMillis());
	private static final CreateAndHold<TrackSelector> trackSelector = new Lazy<>(ExoPlayerPlaybackPreparerProvider::getNewTrackSelector);
	private static final CreateAndHold<LoadControl> loadControl = new Lazy<>(ExoPlayerPlaybackPreparerProvider::getNewLoadControl);
	private static final CreateAndHold<ExtractorsFactory> extractorsFactory = new Lazy<>(() -> Mp3Extractor.FACTORY);

	private final IFileUriProvider fileUriProvider;
	private final DataSourceFactoryProvider dataSourceFactoryProvder;
	private final DiskFileCache diskFileCache;
	private final RenderersFactory renderersFactory;
	private final Handler handler;

	public ExoPlayerPlaybackPreparerProvider(Context context, IFileUriProvider fileUriProvider, Library library) {
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

		renderersFactory = new DefaultRenderersFactory(context);

		handler = new Handler(context.getMainLooper());
	}

	@Override
	public int getMaxQueueSize() {
		return 1;
	}

	@Override
	public IPlaybackPreparer providePlaybackPreparer() {
		return new ExoPlayerPlaybackPreparer(
			dataSourceFactoryProvder,
			trackSelector.getObject(),
			loadControl.getObject(),
			renderersFactory,
			extractorsFactory.getObject(),
			handler,
			diskFileCache,
			fileUriProvider);
	}

	private static TrackSelector getNewTrackSelector() {
		return new DefaultTrackSelector();
	}

	private static LoadControl getNewLoadControl() {
		return new DefaultLoadControl(
			new DefaultAllocator(false, C.DEFAULT_BUFFER_SEGMENT_SIZE),
			DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
			maxBufferMs.getObject(),
			DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
			DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
		);
	}
}
