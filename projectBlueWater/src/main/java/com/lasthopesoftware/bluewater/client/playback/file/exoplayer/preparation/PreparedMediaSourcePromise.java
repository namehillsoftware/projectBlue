package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

import android.os.Handler;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.volume.AudioTrackVolumeManager;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CancellationException;

class PreparedMediaSourcePromise
extends
	Promise<PreparedPlayableFile>
implements
	Player.EventListener,
	MediaSourceEventListener,
	Runnable {

	private static final Logger logger = LoggerFactory.getLogger(PreparedMediaSourcePromise.class);

	private final ExoPlayer exoPlayer;
	private final MediaSource mediaSource;
	private final MediaCodecAudioRenderer[] audioRenderers;
	private final BufferingExoPlayer bufferingExoPlayer;
	private final long prepareAt;
	private final CancellationToken cancellationToken = new CancellationToken();

	private boolean isResolved;

	PreparedMediaSourcePromise(ExoPlayer exoPlayer, MediaSource mediaSource, Handler handler, MediaCodecAudioRenderer[] audioRenderers, BufferingExoPlayer bufferingExoPlayer, long prepareAt) {
		this.exoPlayer = exoPlayer;
		this.mediaSource = mediaSource;
		this.audioRenderers = audioRenderers;
		this.bufferingExoPlayer = bufferingExoPlayer;
		this.prepareAt = prepareAt;

		mediaSource.addEventListener(handler, this);
		exoPlayer.addListener(this);
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

		if (playbackState != Player.STATE_READY) return;

		if (exoPlayer.getCurrentPosition() < prepareAt) {
			exoPlayer.seekTo(prepareAt);
			return;
		}

		resolvePlayerAsPrepared();
	}

	@Override
	public void onRepeatModeChanged(int repeatMode) {}

	@Override
	public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {}

	@Override
	public void onPlayerError(ExoPlaybackException error) {}

	@Override
	public void onPositionDiscontinuity(int reason) {}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}

	@Override
	public void onSeekProcessed() {}

	@Override
	public void run() {
		cancellationToken.run();

		exoPlayer.removeListener(this);

		reject(new CancellationException());
	}

	@Override
	public void onMediaPeriodCreated(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {}

	@Override
	public void onMediaPeriodReleased(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {}

	@Override
	public void onLoadStarted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {}

	@Override
	public void onLoadCompleted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
		resolvePlayerAsPrepared();
	}

	@Override
	public void onLoadCanceled(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {}

	@Override
	public void onLoadError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
		if (isResolved) return;
		isResolved = true;

		logger.error("An error occurred while preparing the exo player!", error);

		exoPlayer.removeListener(this);

		reject(new PlaybackException(new EmptyPlaybackHandler(0), error));
	}

	@Override
	public void onReadingStarted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {}

	@Override
	public void onUpstreamDiscarded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {}

	@Override
	public void onDownstreamFormatChanged(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {}

	private void resolvePlayerAsPrepared() {
		isResolved = true;

		exoPlayer.removeListener(this);
		resolve(
			new PreparedPlayableFile(
				new ExoPlayerPlaybackHandler(exoPlayer),
				new AudioTrackVolumeManager(exoPlayer, audioRenderers),
				bufferingExoPlayer));
	}
}
