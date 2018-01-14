package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.bluewater.client.playback.file.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.LoadingExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;

final class ExoPlayerPreparationHandler
implements
	Player.EventListener,
	Runnable {
	private static final Logger logger = LoggerFactory.getLogger(ExoPlayerPlaybackHandler.class);

	private final SimpleExoPlayer exoPlayer;
	private final Messenger<PreparedPlayableFile> messenger;
	private final LoadingExoPlayer loadingExoPlayer;
	private final long prepareAt;
	private final CancellationToken cancellationToken;

	ExoPlayerPreparationHandler(SimpleExoPlayer exoPlayer, LoadingExoPlayer loadingExoPlayer, long prepareAt, Messenger<PreparedPlayableFile> messenger, CancellationToken cancellationToken) {
		this.exoPlayer = exoPlayer;
		this.loadingExoPlayer = loadingExoPlayer;
		this.prepareAt = prepareAt;
		this.messenger = messenger;
		this.cancellationToken = cancellationToken;
		messenger.cancellationRequested(this);

		loadingExoPlayer.promiseBufferedPlaybackFile()
			.excuse(e -> {
				handleError(e);
				return null;
			});
	}

	@Override
	public void run() {
		cancellationToken.run();

		exoPlayer.release();

		messenger.sendRejection(new CancellationException());
	}

	@Override
	public void onTimelineChanged(Timeline timeline, Object manifest) {
	}

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

	}

	@Override
	public void onLoadingChanged(boolean isLoading) {
	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		if (cancellationToken.isCancelled()) return;

		if (playbackState != Player.STATE_READY) return;

		if (exoPlayer.getCurrentPosition() < prepareAt) {
			exoPlayer.seekTo(prepareAt);
			return;
		}

		exoPlayer.removeListener(this);

		messenger.sendResolution(new PreparedPlayableFile(new ExoPlayerPlaybackHandler(exoPlayer), loadingExoPlayer));
	}

	@Override
	public void onRepeatModeChanged(int repeatMode) {
	}

	@Override
	public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

	}

	@Override
	public void onPlayerError(ExoPlaybackException error) {
		handleError(error);
	}

	@Override
	public void onPositionDiscontinuity(int reason) {
	}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
	}

	@Override
	public void onSeekProcessed() {
	}

	private void handleError(Throwable error) {
		logger.error("An error occurred while preparing the exo player!", error);

		exoPlayer.stop();
		exoPlayer.release();
		messenger.sendRejection(error);
	}
}
