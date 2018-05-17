package com.lasthopesoftware.bluewater.client.playback.playlist;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;

import io.reactivex.ObservableEmitter;

public class ExoPlaylistPlayer implements IPlaylistPlayer, Player.EventListener {

	private final ExoPlayer exoPlayer;
	private final ConcatenatingMediaSource concatenatingMediaSource;
	private ObservableEmitter<PositionedPlayingFile> emitter;

	public ExoPlaylistPlayer(ExoPlayer exoPlayer, ConcatenatingMediaSource concatenatingMediaSource) {
		this.exoPlayer = exoPlayer;
		this.concatenatingMediaSource = concatenatingMediaSource;
	}

	@Override
	public void pause() {
		exoPlayer.stop();
	}

	@Override
	public void resume() {
		exoPlayer.setPlayWhenReady(true);
	}

	@Override
	public void setVolume(float volume) {

	}

	@Override
	public boolean isPlaying() {
		return exoPlayer.getPlayWhenReady();
	}

	@Override
	public void subscribe(ObservableEmitter<PositionedPlayingFile> emitter) {
		this.emitter = emitter;

	}

	@Override
	public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

	}

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
	}

	@Override
	public void onLoadingChanged(boolean isLoading) {

	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

	}

	@Override
	public void onRepeatModeChanged(int repeatMode) {

	}

	@Override
	public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

	}

	@Override
	public void onPlayerError(ExoPlaybackException error) {

	}

	@Override
	public void onPositionDiscontinuity(int reason) {
		exoPlayer.getCurrentPeriodIndex();
	}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
	}

	@Override
	public void onSeekProcessed() {

	}
}
