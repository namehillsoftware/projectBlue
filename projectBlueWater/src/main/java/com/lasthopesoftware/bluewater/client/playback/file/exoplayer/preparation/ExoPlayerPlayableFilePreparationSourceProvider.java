package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

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
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.disk.AndroidDiskCacheDirectoryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.disk.IDiskCacheDirectoryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.DiskFileAccessTimeUpdater;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.DiskFileCachePersistence;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.supplier.DiskFileCacheStreamSupplier;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.RemoteFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ExtractorMediaSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.joda.time.Minutes;


public class ExoPlayerPlayableFilePreparationSourceProvider implements IPlayableFilePreparationSourceProvider {

	private static final CreateAndHold<Integer> maxBufferMs = new Lazy<>(() -> (int) Minutes.minutes(5).toStandardDuration().getMillis());
	private static final CreateAndHold<TrackSelector> trackSelector = new Lazy<>(ExoPlayerPlayableFilePreparationSourceProvider::getNewTrackSelector);
	private static final CreateAndHold<LoadControl> loadControl = new Lazy<>(ExoPlayerPlayableFilePreparationSourceProvider::getNewLoadControl);
	private static final CreateAndHold<ExtractorsFactory> extractorsFactory = new Lazy<>(() -> Mp3Extractor.FACTORY);

	private final BestMatchUriProvider bestMatchUriProvider;
	private final RemoteFileUriProvider remoteFileUriProvider;
	private final ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider;
	private final RenderersFactory renderersFactory;
	private final Handler handler;
	private final DiskFileCache diskFileCache;

	public ExoPlayerPlayableFilePreparationSourceProvider(Context context, BestMatchUriProvider bestMatchUriProvider, RemoteFileUriProvider remoteFileUriProvider, Library library) {
		this.bestMatchUriProvider = bestMatchUriProvider;
		this.remoteFileUriProvider = remoteFileUriProvider;

		final AudioCacheConfiguration audioCacheConfiguration = new AudioCacheConfiguration(library);
		final CachedFilesProvider cachedFilesProvider = new CachedFilesProvider(context, audioCacheConfiguration);
		final DiskFileAccessTimeUpdater diskFileAccessTimeUpdater = new DiskFileAccessTimeUpdater(context);
		final IDiskCacheDirectoryProvider diskCacheDirectoryProvider = new AndroidDiskCacheDirectoryProvider(context);
		final DiskFileCacheStreamSupplier diskFileCacheStream = new DiskFileCacheStreamSupplier(
			diskCacheDirectoryProvider,
			audioCacheConfiguration,
			new DiskFileCachePersistence(
				context,
				diskCacheDirectoryProvider,
				audioCacheConfiguration,
				cachedFilesProvider,
				diskFileAccessTimeUpdater),
			cachedFilesProvider);

		extractorMediaSourceFactoryProvider = new ExtractorMediaSourceFactoryProvider(context, library, diskFileCacheStream);


		diskFileCache =
			new DiskFileCache(
				context,
				diskCacheDirectoryProvider,
				audioCacheConfiguration,
				diskFileCacheStream,
				cachedFilesProvider,
				diskFileAccessTimeUpdater);

		renderersFactory = new DefaultRenderersFactory(context);

		handler = new Handler(context.getMainLooper());
	}

	@Override
	public int getMaxQueueSize() {
		return 1;
	}

	@Override
	public PlayableFilePreparationSource providePlayableFilePreparationSource() {
		return new ExoPlayerPlaybackPreparer(
			extractorMediaSourceFactoryProvider,
			trackSelector.getObject(),
			loadControl.getObject(),
			renderersFactory,
			handler,
			diskFileCache,
			bestMatchUriProvider,
			remoteFileUriProvider);
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
			DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
			DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES,
			DefaultLoadControl.DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS);
	}
}
