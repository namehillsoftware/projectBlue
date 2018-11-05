package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.volume.AudioTrackVolumeManager;
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

	private final ExoPlayer exoPlayer;
	private final Messenger<PreparedPlayableFile> messenger;
	private final MediaCodecAudioRenderer[] audioRenderers;
	private final BufferingExoPlayer bufferingExoPlayer;
	private final long prepareAt;
	private final CancellationToken cancellationToken;

	private boolean isResolved;

	ExoPlayerPreparationHandler(ExoPlayer exoPlayer, MediaCodecAudioRenderer[] audioRenderers, BufferingExoPlayer bufferingExoPlayer, long prepareAt, Messenger<PreparedPlayableFile> messenger, CancellationToken cancellationToken) {
		this.exoPlayer = exoPlayer;
		this.audioRenderers = audioRenderers;
		this.bufferingExoPlayer = bufferingExoPlayer;
		this.prepareAt = prepareAt;
		this.messenger = messenger;
		this.cancellationToken = cancellationToken;
		messenger.cancellationRequested(this);

		bufferingExoPlayer.promiseBufferedPlaybackFile()
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
	public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {}

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {}

	@Override
	public void onLoadingChanged(boolean isLoading) {}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		if (isResolved || cancellationToken.isCancelled()) return;

		if (playbackState != Player.STATE_READY) {
			logger.warn("Player went into state " + playbackStateToString(playbackState) + " before it was prepared!");
			return;
		}

		final long currentPosition = exoPlayer.getCurrentPosition();
		if (currentPosition < prepareAt) {
			if (currentPosition < 0) {
				logger.warn("Player was prepared with a negative current position of " + currentPosition + "!");
			}

			exoPlayer.seekTo(prepareAt);
			return;
		}

		isResolved = true;

		exoPlayer.removeListener(this);
		messenger.sendResolution(
			new PreparedPlayableFile(
				new ExoPlayerPlaybackHandler(exoPlayer),
				new AudioTrackVolumeManager(exoPlayer, audioRenderers),
				bufferingExoPlayer));
	}

	@Override
	public void onRepeatModeChanged(int repeatMode) {}

	@Override
	public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {}

	@Override
	public void onPlayerError(ExoPlaybackException error) {
		handleError(error);
	}

	@Override
	public void onPositionDiscontinuity(int reason) {}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}

	@Override
	public void onSeekProcessed() {}

	private void handleError(Throwable error) {
		if (isResolved) return;
		isResolved = true;

		logger.error("An error occurred while preparing the exo player!", error);

		exoPlayer.stop();
		exoPlayer.release();
		messenger.sendRejection(new PlaybackException(new EmptyPlaybackHandler(0), error));
	}

	private static String playbackStateToString(int playbackState) {
		switch (playbackState) {
			case Player.STATE_BUFFERING: return "buffering";
			case Player.STATE_ENDED: return "ended";
			case Player.STATE_IDLE: return "idle";
			case Player.STATE_READY: return "ready";
			default: return "unknown";
		}
	}
}
