package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.single;

import android.os.Handler;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.QueueMediaSources;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ExtractorMediaSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;
import com.namehillsoftware.handoff.promises.Promise;

public class SingleExoPlayerPlaybackPreparer implements PlayableFilePreparationSource {

	private final ExoPlayer exoPlayer;
	private final ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider;
	private final QueueMediaSources mediaSourcesQueue;
	private final ManagePlayableFileVolume playableFileVolumeManager;
	private final Handler handler;
	private final BestMatchUriProvider bestMatchUriProvider;

	public SingleExoPlayerPlaybackPreparer(ExoPlayer exoPlayer, ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider, QueueMediaSources mediaSourcesQueue, ManagePlayableFileVolume playableFileVolumeManager, Handler handler, BestMatchUriProvider bestMatchUriProvider) {
		this.exoPlayer = exoPlayer;
		this.extractorMediaSourceFactoryProvider = extractorMediaSourceFactoryProvider;
		this.mediaSourcesQueue = mediaSourcesQueue;
		this.playableFileVolumeManager = playableFileVolumeManager;
		this.handler = handler;
		this.bestMatchUriProvider = bestMatchUriProvider;
	}

	@Override
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
		return bestMatchUriProvider.promiseFileUri(serviceFile)
			.eventually(uri -> {
				final BufferingExoPlayer bufferingExoPlayer = new BufferingExoPlayer();

				final MediaSource mediaSource =
					extractorMediaSourceFactoryProvider
						.getFactory(uri)
						.createMediaSource(uri);

				final PreparedMediaSourcePromise preparedMediaSourcePromise = new PreparedMediaSourcePromise(
					exoPlayer,
					mediaSource,
					handler,
					playableFileVolumeManager,
					bufferingExoPlayer,
					preparedAt);

				mediaSourcesQueue.enqueueMediaSource(mediaSource);

				return preparedMediaSourcePromise;
			});
	}
}
