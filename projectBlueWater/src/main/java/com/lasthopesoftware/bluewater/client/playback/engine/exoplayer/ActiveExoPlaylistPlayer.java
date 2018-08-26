package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.IActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;

import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;

public class ActiveExoPlaylistPlayer implements IActivePlayer {
	private final ExoPlayer exoPlayer;
	private final ConnectableObservable<PositionedPlayingFile> playingFileObservable;

	public ActiveExoPlaylistPlayer(ExoPlayer exoPlayer) {
		this.exoPlayer = exoPlayer;
		playingFileObservable = Observable.<PositionedPlayingFile>create(emitter ->
			exoPlayer.addListener(new Player.EventListener() {
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
					if (reason != Player.DISCONTINUITY_REASON_PERIOD_TRANSITION) return;

					emitter.onNext(new PositionedPlayingFile(new PlayingFile() {
						@Override
						public Promise<PlayableFile> promisePause() {
							return null;
						}

						@Override
						public ProgressingPromise<Duration, PlayedFile> promisePlayedFile() {
							return null;
						}

						@Override
						public Duration getDuration() {
							return null;
						}
					}, new ManagePlayableFileVolume() {
						@Override
						public float setVolume(float volume) {
							return 0;
						}

						@Override
						public float getVolume() {
							return 0;
						}
					}, new PositionedFile(1, new ServiceFile(1))));
				}

				@Override
				public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

				}

				@Override
				public void onSeekProcessed() {

				}
		})).publish();
		playingFileObservable.connect();
		exoPlayer.setPlayWhenReady(true);
	}

	@Override
	public void pause() {
		this.exoPlayer.setPlayWhenReady(false);
	}

	@Override
	public void resume() {
		this.exoPlayer.setPlayWhenReady(true);
	}

	@Override
	public ConnectableObservable<PositionedPlayingFile> observe() {
		return playingFileObservable;
	}
}
