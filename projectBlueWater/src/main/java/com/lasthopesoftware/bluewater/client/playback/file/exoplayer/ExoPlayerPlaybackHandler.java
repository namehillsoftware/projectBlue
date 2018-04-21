package com.lasthopesoftware.bluewater.client.playback.file.exoplayer;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.ExoPlayerFileProgressReader;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.events.ExoPlayerPlaybackCompletedNotifier;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.events.ExoPlayerPlaybackErrorNotifier;
import com.lasthopesoftware.bluewater.client.playback.file.progress.PollingProgressSource;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

public class ExoPlayerPlaybackHandler
implements
	PlayableFile,
	PlayingFile,
	Player.EventListener,
	MessengerOperator<PlayableFile>,
	Runnable
{
	private static final Logger logger = LoggerFactory.getLogger(ExoPlayerPlaybackHandler.class);

	private final ExoPlayer exoPlayer;
	private final Duration duration;

	private Messenger<PlayableFile> playbackHandlerMessenger;
	private boolean isPlaying;

	private final CreateAndHold<PollingProgressSource> exoPlayerPositionSource = new AbstractSynchronousLazy<PollingProgressSource>() {
		@Override
		protected PollingProgressSource create() {
			final ExoPlayerPlaybackCompletedNotifier completedNotifier = new ExoPlayerPlaybackCompletedNotifier();
			exoPlayer.addListener(completedNotifier);

			final ExoPlayerPlaybackErrorNotifier errorNotifier = new ExoPlayerPlaybackErrorNotifier(ExoPlayerPlaybackHandler.this);
			exoPlayer.addListener(errorNotifier);

			return new PollingProgressSource<>(
				new ExoPlayerFileProgressReader(exoPlayer),
				completedNotifier,
				errorNotifier,
				Duration.millis(100));
		}
	};

	public ExoPlayerPlaybackHandler(ExoPlayer exoPlayer) {
		this.exoPlayer = exoPlayer;
		exoPlayer.addListener(this);
		duration = Duration.millis(exoPlayer.getDuration());

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
	public Promise<PlayableFile> promisePause() {
		pause();
		return new Promise<>((PlayableFile)this);
	}

	@Override
	public Observable<Duration> observeProgress(Duration observationPeriod) {
		return Observable
			.create(exoPlayerPositionSource.getObject().observePeriodically(observationPeriod))
			.sample(observationPeriod.getMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	public Duration getDuration() {
		return duration;
	}

	@Override
	public Promise<PlayingFile> promisePlayback() {
		exoPlayer.setPlayWhenReady(true);
		isPlaying = true;
		return new Promise<PlayingFile>(this);
	}

	@Override
	public void close() {
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
		handlePlaybackError(error);
	}

	private void handlePlaybackError(Throwable error) {
		logger.error("A player error has occurred", error);
		playbackHandlerMessenger.sendRejection(new ExoPlayerException(this, error));
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

		if (exoPlayerPositionSource.isCreated())
			exoPlayerPositionSource.getObject().close();

		exoPlayer.release();
	}
}
