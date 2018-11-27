package com.lasthopesoftware.bluewater.client.playback.engine.preparation;

import android.os.Handler;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.RenderersFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.QueueMediaSources;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.LookupSelectedPlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ExtractorMediaSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.single.SingleExoPlayerSourcePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;
import com.namehillsoftware.handoff.promises.Promise;

public class PreparedPlaybackQueueFeederBuilder implements BuildPreparedPlaybackQueueFeeder {

	private final LookupSelectedPlaybackEngineType selectedPlaybackEngineTypeLookup;
	private final Handler handler;
	private final BestMatchUriProvider bestMatchUriProvider;
	private final ExtractorMediaSourceFactoryProvider mediaSourceFactoryProvider;
	private final ExoPlayer exoPlayer;
	private final QueueMediaSources mediaSourcesQueue;
	private final RenderersFactory renderersFactory;
	private final ManagePlayableFileVolume playableFileVolumeManager;

	public PreparedPlaybackQueueFeederBuilder(
		LookupSelectedPlaybackEngineType selectedPlaybackEngineTypeLookup,
		Handler handler,
		BestMatchUriProvider bestMatchUriProvider,
		ExtractorMediaSourceFactoryProvider mediaSourceFactoryProvider,
		ExoPlayer exoPlayer,
		QueueMediaSources mediaSourcesQueue,
		RenderersFactory renderersFactory, ManagePlayableFileVolume playableFileVolumeManager) {

		this.selectedPlaybackEngineTypeLookup = selectedPlaybackEngineTypeLookup;
		this.handler = handler;
		this.bestMatchUriProvider = bestMatchUriProvider;
		this.mediaSourceFactoryProvider = mediaSourceFactoryProvider;
		this.exoPlayer = exoPlayer;
		this.mediaSourcesQueue = mediaSourcesQueue;
		this.renderersFactory = renderersFactory;
		this.playableFileVolumeManager = playableFileVolumeManager;
	}

	@Override
	public Promise<IPlayableFilePreparationSourceProvider> build(Library library) {
		return this.selectedPlaybackEngineTypeLookup.promiseSelectedPlaybackEngineType()
			.then(playbackEngineType -> {
				switch (playbackEngineType) {
					case SingleExoPlayer:
						return new SingleExoPlayerSourcePreparationSourceProvider(
							handler,
							bestMatchUriProvider,
							mediaSourceFactoryProvider,
							exoPlayer,
							mediaSourcesQueue,
							playableFileVolumeManager);
					case ExoPlayer:
					default:
						return new ExoPlayerPlayableFilePreparationSourceProvider(
							handler,
							bestMatchUriProvider,
							mediaSourceFactoryProvider,
							renderersFactory);
				}
			});
	}
}
