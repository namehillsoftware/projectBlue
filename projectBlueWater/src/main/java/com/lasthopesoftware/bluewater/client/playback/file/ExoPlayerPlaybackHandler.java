package com.lasthopesoftware.bluewater.client.playback.file;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ExoPlayerPlaybackHandler
implements
	PlayableFile,
	Player.EventListener,
	MessengerOperator<PlayableFile>,
	MediaSourceEventListener,
	Runnable
{
	private static final Logger logger = LoggerFactory.getLogger(ExoPlayerPlaybackHandler.class);

	private final SimpleExoPlayer exoPlayer;
	private final Promise<PlayableFile> playbackHandlerPromise;
	private Messenger<PlayableFile> playbackHandlerMessenger;
	private boolean isPlaying;

	public ExoPlayerPlaybackHandler(SimpleExoPlayer exoPlayer) {
		this.exoPlayer = exoPlayer;
		exoPlayer.addListener(this);

		this.playbackHandlerPromise = new Promise<>((MessengerOperator<PlayableFile>) this);
	}

	@Override
	public boolean isPlaying() {
		return isPlaying;
	}

	@Override
	public void pause() {
		exoPlayer.stop();
		isPlaying = false;
	}

	@Override
	public void setVolume(float volume) {
		exoPlayer.setVolume(volume);
	}

	@Override
	public float getVolume() {
		return exoPlayer.getVolume();
	}

	@Override
	public long getCurrentPosition() {
		return exoPlayer.getCurrentPosition();
	}

	@Override
	public long getDuration() {
		return exoPlayer.getDuration();
	}

	@Override
	public Promise<PlayableFile> promisePlayback() {
		exoPlayer.setPlayWhenReady(true);
		isPlaying = true;
		return playbackHandlerPromise;
	}

	@Override
	public void close() throws IOException {
		run();
	}

	@Override
	public void send(Messenger<PlayableFile> playbackHandlerMessenger) {
		this.playbackHandlerMessenger = playbackHandlerMessenger;

		playbackHandlerMessenger.cancellationRequested(this);
	}

	@Override
	public void onTimelineChanged(Timeline timeline, Object manifest) {
		logger.debug("Timeline changed");
	}

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
		logger.debug("Tracks changed");
	}

	@Override
	public void onLoadingChanged(boolean isLoading) {
		logger.debug("Loading changed to " + isLoading);
	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		logger.debug("Playback state has changed to " + playbackState);

		if (isPlaying && playbackState == Player.STATE_IDLE) {
			logger.warn("The player was playing, but it transitioned to idle! Restarting the player...");
			pause();
			exoPlayer.setPlayWhenReady(true);
		}

		if (playbackState != Player.STATE_ENDED) return;

		isPlaying = false;

		playbackHandlerMessenger.sendResolution(this);
		exoPlayer.removeListener(this);
	}

	@Override
	public void onRepeatModeChanged(int repeatMode) {
		logger.debug("Repeat mode has changed");
	}

	@Override
	public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

	}

	@Override
	public void onPlayerError(ExoPlaybackException error) {
		logger.error("A player error has occurred", error);
		playbackHandlerMessenger.sendRejection(error);
	}

	@Override
	public void onPositionDiscontinuity(int reason) {

	}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
		logger.debug("Playback parameters have changed");
	}

	@Override
	public void onSeekProcessed() {

	}

	@Override
	public void run() {
		isPlaying = false;
		exoPlayer.setPlayWhenReady(false);
		exoPlayer.stop();
		exoPlayer.release();
	}

	@Override
	public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs) {

	}

	@Override
	public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {

	}

	@Override
	public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {

	}

	@Override
	public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {
		logger.error("A loading error occurred", error);
		playbackHandlerMessenger.sendRejection(error);
	}

	@Override
	public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {

	}

	@Override
	public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeMs) {

	}
}
