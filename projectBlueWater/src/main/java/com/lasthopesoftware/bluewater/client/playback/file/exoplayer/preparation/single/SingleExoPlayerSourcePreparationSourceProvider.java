package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.single;

import android.os.Handler;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.QueueMediaSources;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ExtractorMediaSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;


public class SingleExoPlayerSourcePreparationSourceProvider implements IPlayableFilePreparationSourceProvider {

	private final Handler handler;
	private final BestMatchUriProvider bestMatchUriProvider;
	private final ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider;
	private final ManagePlayableFileVolume playableFileVolumeManager;
	private final ExoPlayer exoPlayer;
	private final QueueMediaSources mediaSourcesQueue;

	public SingleExoPlayerSourcePreparationSourceProvider(
		Handler handler,
		BestMatchUriProvider bestMatchUriProvider,
		ExtractorMediaSourceFactoryProvider mediaSourceFactoryProvider,
		ExoPlayer exoPlayer,
		QueueMediaSources mediaSourcesQueue,
		ManagePlayableFileVolume playableFileVolumeManager) {

		this.handler = handler;
		this.bestMatchUriProvider = bestMatchUriProvider;
		this.extractorMediaSourceFactoryProvider = mediaSourceFactoryProvider;
		this.exoPlayer = exoPlayer;
		this.mediaSourcesQueue = mediaSourcesQueue;
		this.playableFileVolumeManager = playableFileVolumeManager;
	}

	@Override
	public int getMaxQueueSize() {
		return 5;
	}

	@Override
	public PlayableFilePreparationSource providePlayableFilePreparationSource() {
		return new SingleExoPlayerPlaybackPreparer(
			exoPlayer,
			extractorMediaSourceFactoryProvider,
			mediaSourcesQueue,
			playableFileVolumeManager,
			handler,
			bestMatchUriProvider);
	}
}
