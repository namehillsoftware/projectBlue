package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer;

import android.net.Uri;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.bluewater.client.playback.file.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.BufferingExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource.MediaSourceProvider;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;
import com.lasthopesoftware.messenger.promises.response.PromisedResponse;

import java.io.IOException;
import java.util.concurrent.CancellationException;

final class ExoPlayerPreparerTask implements PromisedResponse<Uri, PreparedPlaybackFile> {

	private final int prepareAt;
	private final MediaSourceProvider mediaSourceProvider;
	private IPlaybackInitialization<ExoPlayer> playbackInitialization;

	ExoPlayerPreparerTask(int prepareAt, MediaSourceProvider mediaSourceProvider, IPlaybackInitialization<ExoPlayer> playbackInitialization) {
		this.prepareAt = prepareAt;
		this.mediaSourceProvider = mediaSourceProvider;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public Promise<PreparedPlaybackFile> promiseResponse(Uri uri) throws Throwable {
		return new Promise<>(new ExoPlayerPreparationOperator(uri, mediaSourceProvider, playbackInitialization, prepareAt));
	}

	private static final class ExoPlayerPreparationOperator implements MessengerOperator<PreparedPlaybackFile> {
		private final Uri uri;
		private final MediaSourceProvider mediaSourceProvider;
		private final IPlaybackInitialization<ExoPlayer> playbackInitialization;
		private final int prepareAt;

		ExoPlayerPreparationOperator(Uri uri, MediaSourceProvider mediaSourceProvider, IPlaybackInitialization<ExoPlayer> playbackInitialization, int prepareAt) {
			this.uri = uri;
			this.mediaSourceProvider = mediaSourceProvider;
			this.playbackInitialization = playbackInitialization;
			this.prepareAt = prepareAt;
		}

		@Override
		public void send(Messenger<PreparedPlaybackFile> messenger) {
			final CancellationToken cancellationToken = new CancellationToken();
			messenger.cancellationRequested(cancellationToken);

			if (cancellationToken.isCancelled()) {
				messenger.sendRejection(new CancellationException());
				return;
			}

			final ExoPlayer exoPlayer;
			try {
				exoPlayer = playbackInitialization.initializeMediaPlayer(uri);
			} catch (IOException e) {
				messenger.sendRejection(e);
				return;
			}

			if (cancellationToken.isCancelled()) {
				exoPlayer.release();
				messenger.sendRejection(new CancellationException());
				return;
			}

			final ExoPlayerPreparationHandler exoPlayerPreparationHandler =
				new ExoPlayerPreparationHandler(exoPlayer, prepareAt, messenger, cancellationToken);

			exoPlayer.addListener(exoPlayerPreparationHandler);

			if (cancellationToken.isCancelled()) return;

			try {
				exoPlayer.prepare(mediaSourceProvider.getMediaSource(uri));
			} catch (IllegalStateException e) {
				messenger.sendRejection(e);
			}
		}
	}

	private static final class ExoPlayerPreparationHandler
		implements
			Player.EventListener,
			Runnable
	{
		private final ExoPlayer exoPlayer;
		private final Messenger<PreparedPlaybackFile> messenger;
		private final int prepareAt;
		private final CancellationToken cancellationToken;

		private ExoPlayerPreparationHandler(ExoPlayer exoPlayer, int prepareAt, Messenger<PreparedPlaybackFile> messenger, CancellationToken cancellationToken) {
			this.exoPlayer = exoPlayer;
			this.prepareAt = prepareAt;
			this.messenger = messenger;
			this.cancellationToken = cancellationToken;
			messenger.cancellationRequested(this);
		}

		@Override
		public void run() {
			cancellationToken.run();

			exoPlayer.release();

			messenger.sendRejection(new CancellationException());
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
			if (cancellationToken.isCancelled()) return;

			if (playbackState != Player.STATE_READY) return;

			if (prepareAt > 0 && exoPlayer.getContentPosition() != prepareAt) {
				exoPlayer.seekTo(prepareAt);
				return;
			}

			messenger.sendResolution(new PreparedPlaybackFile(new ExoPlayerPlaybackHandler(exoPlayer), new BufferingExoPlayer()));
		}

		@Override
		public void onRepeatModeChanged(int repeatMode) {

		}

		@Override
		public void onPlayerError(ExoPlaybackException error) {
			exoPlayer.release();
			messenger.sendRejection(error);
		}

		@Override
		public void onPositionDiscontinuity() {

		}

		@Override
		public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
		}
	}
}
