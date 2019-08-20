package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.single;

import android.os.Handler;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.QueueMediaSources;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;
import com.namehillsoftware.handoff.promises.Promise;

public class SingleExoPlayerPlaybackPreparer implements PlayableFilePreparationSource {

	private final ExoPlayer exoPlayer;
	private final SpawnMediaSources spawnMediaSources;
	private final QueueMediaSources mediaSourcesQueue;
	private final ManagePlayableFileVolume playableFileVolumeManager;
	private final Handler handler;
	private final IFileUriProvider uriProvider;

	public SingleExoPlayerPlaybackPreparer(ExoPlayer exoPlayer, SpawnMediaSources mediaSources, QueueMediaSources mediaSourcesQueue, ManagePlayableFileVolume playableFileVolumeManager, Handler handler, IFileUriProvider uriProvider) {
		this.exoPlayer = exoPlayer;
		this.spawnMediaSources = mediaSources;
		this.mediaSourcesQueue = mediaSourcesQueue;
		this.playableFileVolumeManager = playableFileVolumeManager;
		this.handler = handler;
		this.uriProvider = uriProvider;
	}

	@Override
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
		return uriProvider.promiseFileUri(serviceFile)
			.eventually(uri -> {
				final MediaSource mediaSource = spawnMediaSources.getNewMediaSource(uri);

				final BufferingExoPlayer bufferingExoPlayer = new BufferingExoPlayer(handler, mediaSource);

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
