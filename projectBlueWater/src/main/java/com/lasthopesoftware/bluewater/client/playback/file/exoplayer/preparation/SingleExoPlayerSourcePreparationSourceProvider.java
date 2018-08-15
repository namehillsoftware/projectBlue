package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

import android.os.Handler;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.RenderersFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.QueueMediaSources;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ExtractorMediaSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;


public class SingleExoPlayerSourcePreparationSourceProvider implements IPlayableFilePreparationSourceProvider {

	private final Handler handler;
	private final BestMatchUriProvider bestMatchUriProvider;
	private final ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider;
	private final RenderersFactory renderersFactory;
	private final ExoPlayer exoPlayer;
	private final QueueMediaSources mediaSourcesQueue;

	public SingleExoPlayerSourcePreparationSourceProvider(
		Handler handler,
		BestMatchUriProvider bestMatchUriProvider,
		ExtractorMediaSourceFactoryProvider mediaSourceFactoryProvider,
		ExoPlayer exoPlayer,
		QueueMediaSources mediaSourcesQueue,
		RenderersFactory renderersFactory) {

		this.handler = handler;
		this.bestMatchUriProvider = bestMatchUriProvider;
		this.extractorMediaSourceFactoryProvider = mediaSourceFactoryProvider;
		this.renderersFactory = renderersFactory;
		this.exoPlayer = exoPlayer;
		this.mediaSourcesQueue = mediaSourcesQueue;
	}

	@Override
	public int getMaxQueueSize() {
		return Integer.MAX_VALUE;
	}

	@Override
	public PlayableFilePreparationSource providePlayableFilePreparationSource() {
		return new SingleExoPlayerPlaybackPreparer(
			exoPlayer,
			extractorMediaSourceFactoryProvider,
			mediaSourcesQueue,
			renderersFactory,
			handler,
			bestMatchUriProvider);
	}
}
