package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.playback;

import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.ExoPlayerFileProgressReader;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;
import com.namehillsoftware.lazyj.Lazy;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.net.ProtocolException;

public class PromisedPlayedExoPlayer
extends
	ProgressingPromise<Duration, PlayedFile>
implements
	PlayedFile,
	Player.EventListener {


	private static final Lazy<PeriodFormatter> minutesAndSecondsFormatter =
		new Lazy<>(() ->
			new PeriodFormatterBuilder()
				.appendMinutes()
				.appendSeparator(":")
				.minimumPrintedDigits(2)
				.maximumParsedDigits(2)
				.appendSeconds()
				.toFormatter());

	private static final Logger logger = LoggerFactory.getLogger(PromisedPlayedExoPlayer.class);

	private final ExoPlayer exoPlayer;
	private final ExoPlayerFileProgressReader progressReader;
	private final ExoPlayerPlaybackHandler handler;

	public PromisedPlayedExoPlayer(ExoPlayer exoPlayer, ExoPlayerFileProgressReader progressReader, ExoPlayerPlaybackHandler handler) {
		this.exoPlayer = exoPlayer;
		this.progressReader = progressReader;
		this.handler = handler;
		this.exoPlayer.addListener(this);
	}

	@Override
	public Duration getProgress() {
		return progressReader.getProgress();
	}

	@Override
	public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {}

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {}

	@Override
	public void onLoadingChanged(boolean isLoading) {}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		if (playbackState == Player.STATE_IDLE && handler.isPlaying()) {
			final PeriodFormatter formatter = minutesAndSecondsFormatter.getObject();

			final Duration progress = getProgress();

			logger.warn(
					"The player was playing, but it transitioned to idle! " +
							"Playback progress: " + progress.toPeriod().toString(formatter) + " / " + handler.getDuration().toPeriod().toString(formatter) + ". ");


			if (playWhenReady) {
				logger.warn("The file is set to playWhenReady, waiting for playback to resume.");
				return;
			}

			logger.warn("The file is not set to playWhenReady, triggering playback completed");
			removeListener();
			resolve(this);
			return;
		}

		if (playbackState != Player.STATE_ENDED) return;

		removeListener();
		resolve(this);
	}

	@Override
	public void onRepeatModeChanged(int repeatMode) {}

	@Override
	public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {}

	@Override
	public void onPlayerError(ExoPlaybackException error) {
		removeListener();

		if (error.getCause() instanceof EOFException) {
			logger.warn("The file ended unexpectedly. Completing playback", error);
			resolve(this);
			return;
		}

		if (error.getCause() instanceof ProtocolException) {
			final ProtocolException protocolException = (ProtocolException) error.getCause();
			if (protocolException.getMessage() != null && protocolException.getMessage().contains("unexpected end of stream")) {
				logger.warn("The stream ended unexpectedly, completing playback", error);
				resolve(this);
				return;
			}
		}

		logger.error("A player error has occurred", error);

		reject(new ExoPlayerException(handler, error));
	}

	@Override
	public void onPositionDiscontinuity(int reason) {}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}

	@Override
	public void onSeekProcessed() {}

	private void removeListener() {
		exoPlayer.removeListener(this);
	}
}
