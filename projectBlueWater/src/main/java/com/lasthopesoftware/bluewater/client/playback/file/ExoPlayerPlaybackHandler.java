package com.lasthopesoftware.bluewater.client.playback.file;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;
import com.lasthopesoftware.messenger.promises.Promise;

import java.io.IOException;

public class ExoPlayerPlaybackHandler
implements
	IPlaybackHandler,
	Player.EventListener,
	MessengerOperator<IPlaybackHandler>,
	Runnable
{
	private final SimpleExoPlayer exoPlayer;
	private final Promise<IPlaybackHandler> playbackHandlerPromise;
	private Messenger<IPlaybackHandler> playbackHandlerMessenger;

	public ExoPlayerPlaybackHandler(SimpleExoPlayer exoPlayer) {
		this.exoPlayer = exoPlayer;
		exoPlayer.addListener(this);

		this.playbackHandlerPromise = new Promise<>((MessengerOperator<IPlaybackHandler>) this);
	}

	@Override
	public boolean isPlaying() {
		return false;
	}

	@Override
	public void pause() {
		exoPlayer.setPlayWhenReady(false);
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
	public Promise<IPlaybackHandler> promisePlayback() {
		exoPlayer.setPlayWhenReady(true);
		return playbackHandlerPromise;
	}

	@Override
	public void close() throws IOException {
		exoPlayer.release();
	}

	@Override
	public void send(Messenger<IPlaybackHandler> playbackHandlerMessenger) {
		this.playbackHandlerMessenger = playbackHandlerMessenger;

		playbackHandlerMessenger.cancellationRequested(this);
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
		if (playbackState != Player.STATE_ENDED) return;

		playbackHandlerMessenger.sendResolution(this);
		exoPlayer.removeListener(this);
	}

	@Override
	public void onRepeatModeChanged(int repeatMode) {

	}

	@Override
	public void onPlayerError(ExoPlaybackException error) {
		playbackHandlerMessenger.sendRejection(error);
	}

	@Override
	public void onPositionDiscontinuity() {

	}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

	}

	@Override
	public void run() {
		exoPlayer.setPlayWhenReady(false);
		exoPlayer.release();
	}
}
