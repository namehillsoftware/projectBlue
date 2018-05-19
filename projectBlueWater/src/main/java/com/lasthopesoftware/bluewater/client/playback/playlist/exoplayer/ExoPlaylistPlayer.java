package com.lasthopesoftware.bluewater.client.playback.playlist.exoplayer;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;

import java.util.List;

import io.reactivex.ObservableEmitter;

public class ExoPlaylistPlayer implements IPlaylistPlayer, Player.EventListener {

	private final ExoPlayer exoPlayer;
	private final List<ServiceFile> serviceFiles;
	private ObservableEmitter<PositionedPlayingFile> emitter;

	public ExoPlaylistPlayer(ExoPlayer exoPlayer, List<ServiceFile> serviceFiles) {
		this.exoPlayer = exoPlayer;
		exoPlayer.addListener(this);
		this.serviceFiles = serviceFiles;
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
		if (emitter == null) return;

		final int position = exoPlayer.getCurrentPeriodIndex();
		emitter.onNext(new PositionedPlayingFile(
			position,
			new PlayingFile() {
				@Override
				public Promise<PlayableFile> promisePause() {
					return Promise.empty();
				}

				@Override
				public ProgressingPromise<Duration, PlayedFile> promisePlayedFile() {
					return null;
				}

				@Override
				public Duration getDuration() {
					return null;
				}
			},
			null,
			serviceFiles.get(position)
		));
	}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
	}

	@Override
	public void onSeekProcessed() {

	}
}
