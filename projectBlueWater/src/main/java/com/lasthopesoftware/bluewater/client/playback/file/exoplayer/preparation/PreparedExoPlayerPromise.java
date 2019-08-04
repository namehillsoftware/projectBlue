package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

import android.net.Uri;
import android.os.Handler;
import com.annimon.stream.Stream;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.AudioRenderingEventListener;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.MetadataOutputLogger;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.TextOutputLogger;
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.volume.AudioTrackVolumeManager;
import com.lasthopesoftware.compilation.DebugFlag;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;

final class PreparedExoPlayerPromise
extends
	Promise<PreparedPlayableFile>
implements
	Player.EventListener,
	Runnable {

	private static final CreateAndHold<TextOutputLogger> lazyTextOutputLogger = new Lazy<>(TextOutputLogger::new);
	private static final CreateAndHold<MetadataOutputLogger> lazyMetadataOutputLogger = new Lazy<>(MetadataOutputLogger::new);

	private static final Logger logger = LoggerFactory.getLogger(ExoPlayerPlaybackHandler.class);

	private final ExoPlayer exoPlayer;
	private final MediaCodecAudioRenderer[] audioRenderers;
	private final BufferingExoPlayer bufferingExoPlayer;
	private final long prepareAt;
	private final CancellationToken cancellationToken = new CancellationToken();

	private boolean isResolved;

	PreparedExoPlayerPromise(SpawnMediaSources mediaSourceProvider,
							 TrackSelector trackSelector,
							 LoadControl loadControl,
							 RenderersFactory renderersFactory,
							 Handler handler,
							 Uri uri,
							 long prepareAt) {

		respondToCancellation(this);

		this.prepareAt = prepareAt;

		if (cancellationToken.isCancelled()) {
			reject(new CancellationException());
			exoPlayer = null;
			audioRenderers = null;
			bufferingExoPlayer = null;
			return;
		}

		audioRenderers =
			Stream.of(renderersFactory.createRenderers(
				handler,
				null,
				DebugFlag.getInstance().isDebugCompilation() ? new AudioRenderingEventListener() : null,
				lazyTextOutputLogger.getObject(),
				lazyMetadataOutputLogger.getObject(),
				null))
				.filter(r -> r instanceof MediaCodecAudioRenderer)
				.map(r -> (MediaCodecAudioRenderer)r)
				.toArray(MediaCodecAudioRenderer[]::new);

		exoPlayer = ExoPlayerFactory.newInstance(
			audioRenderers,
			trackSelector,
			loadControl,
			handler.getLooper());

		if (cancellationToken.isCancelled()) {
			reject(new CancellationException());
			bufferingExoPlayer = null;
			return;
		}


		exoPlayer.addListener(this);

		if (cancellationToken.isCancelled()) {
			bufferingExoPlayer = null;
			return;
		}

		final MediaSource mediaSource =
			mediaSourceProvider.getNewMediaSource(uri);

		bufferingExoPlayer = new BufferingExoPlayer();
		mediaSource.addEventListener(handler, bufferingExoPlayer);

		try {
			exoPlayer.prepare(mediaSource);
		} catch (IllegalStateException e) {
			reject(e);
		}

		bufferingExoPlayer.promiseBufferedPlaybackFile()
			.excuse(e -> {
				handleError(e);
				return null;
			});
	}

	@Override
	public void run() {
		cancellationToken.run();

		if (exoPlayer != null) exoPlayer.release();

		reject(new CancellationException());
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

		isResolved = true;

		exoPlayer.removeListener(this);
		resolve(
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
		reject(new PlaybackException(new EmptyPlaybackHandler(0), error));
	}
}
